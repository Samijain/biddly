package com.biddly.auction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.util.ReflectionTestUtils;

import com.biddly.common.BusinessRuleException;
import com.biddly.user.User;

/**
 * Pure unit tests for the auction lifecycle rules on the entity itself: no Spring, no mocks, no database.
 */
class AuctionTest {

    private static final Instant NOW = Instant.parse("2026-11-04T10:00:00Z");

    @Test
    void publishesDraftThatHasStartedAsActive() {
        var auction = draft(NOW, NOW.plus(1, ChronoUnit.DAYS));

        auction.publish(NOW);

        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.ACTIVE);
    }

    @Test
    void publishesDraftThatStartsLaterAsScheduled() {
        var auction = draft(NOW.plus(1, ChronoUnit.HOURS), NOW.plus(1, ChronoUnit.DAYS));

        auction.publish(NOW);

        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.SCHEDULED);
    }

    @Test
    void rejectsPublishingAnAuctionThatIsNotADraft() {
        var auction = draft(NOW, NOW.plus(1, ChronoUnit.DAYS));
        withStatus(auction, AuctionStatus.ACTIVE);

        assertThatThrownBy(() -> auction.publish(NOW))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("AUCTION_NOT_DRAFT");
        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.ACTIVE);
    }

    @Test
    void rejectsPublishingADraftThatHasAlreadyEnded() {
        var auction = draft(NOW.minus(2, ChronoUnit.DAYS), NOW);

        assertThatThrownBy(() -> auction.publish(NOW))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("AUCTION_ALREADY_ENDED");
        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.DRAFT);
    }

    @ParameterizedTest
    @EnumSource(value = AuctionStatus.class, names = {"DRAFT", "SCHEDULED", "ACTIVE"})
    void cancelsAnAuctionThatHasNotFinished(AuctionStatus status) {
        var auction = draft(NOW, NOW.plus(1, ChronoUnit.DAYS));
        withStatus(auction, status);

        auction.cancel();

        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.CANCELLED);
    }

    @ParameterizedTest
    @EnumSource(value = AuctionStatus.class, names = {"COMPLETED", "CANCELLED"})
    void rejectsCancellingAFinishedAuction(AuctionStatus status) {
        var auction = draft(NOW, NOW.plus(1, ChronoUnit.DAYS));
        withStatus(auction, status);

        assertThatThrownBy(auction::cancel)
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("AUCTION_NOT_CANCELLABLE");
        assertThat(auction.getStatus()).isEqualTo(status);
    }

    private static Auction draft(Instant start, Instant end) {
        return new Auction(new User("Asha", "asha@test.dev"), null, "Vintage clock", "Brass, 1920s", null,
                new BigDecimal("1000.00"), new BigDecimal("50.00"), start, end);
    }

    /** Auction has no status setter on purpose; tests reach other states through reflection instead. */
    private static void withStatus(Auction auction, AuctionStatus status) {
        ReflectionTestUtils.setField(auction, "status", status);
    }
}
