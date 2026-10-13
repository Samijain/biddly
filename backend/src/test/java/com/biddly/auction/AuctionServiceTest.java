package com.biddly.auction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.biddly.auction.dto.CreateAuctionRequest;
import com.biddly.category.CategoryRepository;
import com.biddly.common.BusinessRuleException;
import com.biddly.common.ResourceNotFoundException;
import com.biddly.user.User;
import com.biddly.user.UserRepository;

/**
 * Pure unit tests: no Spring, no database. Dependencies are Mockito mocks and time is frozen.
 * CHALLENGE 2: remove @Disabled once AuctionService.createAuction is implemented.
 */
@Disabled("CHALLENGE 2 - enable after implementing AuctionService.createAuction")
class AuctionServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-13T10:00:00Z");

    private AuctionRepository auctionRepository;
    private UserRepository userRepository;
    private CategoryRepository categoryRepository;
    private AuctionService service;

    @BeforeEach
    void setUp() {
        auctionRepository = mock(AuctionRepository.class);
        userRepository = mock(UserRepository.class);
        categoryRepository = mock(CategoryRepository.class);
        var clock = Clock.fixed(NOW, ZoneOffset.UTC);
        service = new AuctionService(auctionRepository, userRepository, categoryRepository, new AuctionMapper(), clock);

        when(auctionRepository.save(any(Auction.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createsDraftAuctionWithCurrentPriceEqualToStartingPrice() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(new User("Asha", "asha@test.dev")));

        var response = service.createAuction(request(NOW.plus(1, ChronoUnit.HOURS), NOW.plus(2, ChronoUnit.DAYS)));

        assertThat(response.status()).isEqualTo(AuctionStatus.DRAFT);
        assertThat(response.currentPrice()).isEqualByComparingTo("1000.00");
        verify(auctionRepository).save(any(Auction.class));
    }

    @Test
    void rejectsStartTimeInThePast() {
        assertThatThrownBy(() -> service.createAuction(request(NOW.minus(1, ChronoUnit.MINUTES), NOW.plus(1, ChronoUnit.DAYS))))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("START_TIME_IN_PAST");
        verify(auctionRepository, never()).save(any());
    }

    @Test
    void rejectsEndTimeThatIsNotAfterStartTime() {
        var start = NOW.plus(1, ChronoUnit.HOURS);
        assertThatThrownBy(() -> service.createAuction(request(start, start)))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("INVALID_TIME_WINDOW");
        verify(auctionRepository, never()).save(any());
    }

    @Test
    void rejectsUnknownSeller() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createAuction(request(NOW.plus(1, ChronoUnit.HOURS), NOW.plus(1, ChronoUnit.DAYS))))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(auctionRepository, never()).save(any());
    }

    private static CreateAuctionRequest request(Instant start, Instant end) {
        return new CreateAuctionRequest(1L, null, "Vintage clock", "Brass, 1920s", null,
                new BigDecimal("1000.00"), new BigDecimal("50.00"), start, end);
    }
}
