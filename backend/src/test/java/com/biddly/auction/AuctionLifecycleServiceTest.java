package com.biddly.auction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.test.util.ReflectionTestUtils;

import com.biddly.user.User;

/** Pure unit tests: the repository is a Mockito mock and time is frozen at NOW. */
class AuctionLifecycleServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-13T10:00:00Z");

    private AuctionRepository auctionRepository;
    private AuctionLifecycleService service;

    @BeforeEach
    void setUp() {
        auctionRepository = mock(AuctionRepository.class);
        service = new AuctionLifecycleService(auctionRepository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void startsEveryScheduledAuctionThatIsDue() {
        var first = auction(NOW.minus(1, ChronoUnit.MINUTES), NOW.plus(1, ChronoUnit.DAYS), AuctionStatus.SCHEDULED);
        var second = auction(NOW, NOW.plus(2, ChronoUnit.DAYS), AuctionStatus.SCHEDULED);
        when(auctionRepository.findByStatusAndStartTimeLessThanEqual(AuctionStatus.SCHEDULED, NOW)).thenReturn(List.of(first, second));

        var run = service.advance();

        assertThat(first.getStatus()).isEqualTo(AuctionStatus.ACTIVE);
        assertThat(second.getStatus()).isEqualTo(AuctionStatus.ACTIVE);
        assertThat(run.started()).isEqualTo(2);
    }

    @Test
    void completesEveryActiveAuctionThatHasEnded() {
        var ended = auction(NOW.minus(1, ChronoUnit.DAYS), NOW, AuctionStatus.ACTIVE);
        when(auctionRepository.findByStatusAndEndTimeLessThanEqual(AuctionStatus.ACTIVE, NOW)).thenReturn(List.of(ended));

        var run = service.advance();

        assertThat(ended.getStatus()).isEqualTo(AuctionStatus.COMPLETED);
        assertThat(run.completed()).isEqualTo(1);
    }

    @Test
    void asksForBothListsWithTheSameNow() {
        service.advance();

        verify(auctionRepository).findByStatusAndStartTimeLessThanEqual(AuctionStatus.SCHEDULED, NOW);
        verify(auctionRepository).findByStatusAndEndTimeLessThanEqual(AuctionStatus.ACTIVE, NOW);
    }

    @Test
    void doesNothingWhenNothingIsDue() {
        var run = service.advance();

        assertThat(run).isEqualTo(new AuctionLifecycleService.LifecycleRun(0, 0));
    }

    @Test
    void needsNoSaveBecauseTheAuctionsAreManaged() {
        var ended = auction(NOW.minus(1, ChronoUnit.DAYS), NOW, AuctionStatus.ACTIVE);
        when(auctionRepository.findByStatusAndEndTimeLessThanEqual(AuctionStatus.ACTIVE, NOW)).thenReturn(List.of(ended));

        service.advance();

        verify(auctionRepository, never()).save(ArgumentMatchers.any());
        verify(auctionRepository, never()).saveAll(ArgumentMatchers.any());
    }

    private static Auction auction(Instant start, Instant end, AuctionStatus status) {
        var seller = new User("Asha Seller", "asha@test.dev");
        ReflectionTestUtils.setField(seller, "id", 1L);
        var auction = new Auction(seller, null, "Vintage clock", "Brass, 1920s", null,
                new BigDecimal("1000.00"), new BigDecimal("50.00"), start, end);
        ReflectionTestUtils.setField(auction, "status", status);
        return auction;
    }
}
