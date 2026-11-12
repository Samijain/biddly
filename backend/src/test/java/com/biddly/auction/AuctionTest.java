package com.biddly.auction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.util.ReflectionTestUtils;

import com.biddly.category.Category;
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

    @Nested
    class UpdateDetails {

        private final Category watches = new Category("Watches", "watches");
        private final Instant newStart = NOW.plus(2, ChronoUnit.HOURS);
        private final Instant newEnd = NOW.plus(3, ChronoUnit.DAYS);

        @Test
        void replacesEveryDetailOfADraft() {
            var auction = draft(NOW, NOW.plus(1, ChronoUnit.DAYS));

            auction.updateDetails(watches, "Pocket watch", "Silver, 1890s", "https://img.test/watch.jpg",
                    new BigDecimal("2500.00"), new BigDecimal("100.00"), newStart, newEnd);

            assertThat(auction.getCategory()).isSameAs(watches);
            assertThat(auction.getTitle()).isEqualTo("Pocket watch");
            assertThat(auction.getDescription()).isEqualTo("Silver, 1890s");
            assertThat(auction.getCoverImageUrl()).isEqualTo("https://img.test/watch.jpg");
            assertThat(auction.getStartingPrice()).isEqualByComparingTo("2500.00");
            assertThat(auction.getMinimumIncrement()).isEqualByComparingTo("100.00");
            assertThat(auction.getStartTime()).isEqualTo(newStart);
            assertThat(auction.getEndTime()).isEqualTo(newEnd);
            assertThat(auction.getStatus()).isEqualTo(AuctionStatus.DRAFT);
        }

        @Test
        void resetsCurrentPriceToTheNewStartingPrice() {
            var auction = draft(NOW, NOW.plus(1, ChronoUnit.DAYS));

            auction.updateDetails(null, "Vintage clock", null, null,
                    new BigDecimal("750.00"), new BigDecimal("50.00"), newStart, newEnd);

            assertThat(auction.getCurrentPrice()).isEqualByComparingTo("750.00");
        }

        @Test
        void clearsOptionalFieldsThatAreSentAsNull() {
            var auction = draft(NOW, NOW.plus(1, ChronoUnit.DAYS));
            auction.updateDetails(watches, "Vintage clock", "Brass, 1920s", "https://img.test/clock.jpg",
                    new BigDecimal("1000.00"), new BigDecimal("50.00"), newStart, newEnd);

            auction.updateDetails(null, "Vintage clock", null, null,
                    new BigDecimal("1000.00"), new BigDecimal("50.00"), newStart, newEnd);

            assertThat(auction.getCategory()).isNull();
            assertThat(auction.getDescription()).isNull();
            assertThat(auction.getCoverImageUrl()).isNull();
        }

        @ParameterizedTest
        @EnumSource(value = AuctionStatus.class, names = {"SCHEDULED", "ACTIVE", "COMPLETED", "CANCELLED"})
        void rejectsEditingAnAuctionThatIsNotADraft(AuctionStatus status) {
            var auction = draft(NOW, NOW.plus(1, ChronoUnit.DAYS));
            withStatus(auction, status);

            assertThatThrownBy(() -> auction.updateDetails(watches, "Pocket watch", null, null,
                    new BigDecimal("2500.00"), new BigDecimal("100.00"), newStart, newEnd))
                    .isInstanceOf(BusinessRuleException.class)
                    .extracting("code").isEqualTo("AUCTION_NOT_EDITABLE");
            assertThat(auction.getTitle()).isEqualTo("Vintage clock");
            assertThat(auction.getStartingPrice()).isEqualByComparingTo("1000.00");
        }
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
