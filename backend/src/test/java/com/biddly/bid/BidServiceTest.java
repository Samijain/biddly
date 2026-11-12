package com.biddly.bid;

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
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import com.biddly.auction.Auction;
import com.biddly.auction.AuctionRepository;
import com.biddly.auction.AuctionStatus;
import com.biddly.bid.dto.PlaceBidRequest;
import com.biddly.common.BusinessRuleException;
import com.biddly.common.ResourceNotFoundException;
import com.biddly.user.User;
import com.biddly.user.UserRepository;

/** Pure unit tests: no Spring, no database. Dependencies are Mockito mocks and time is frozen. */
class BidServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-12T10:00:00Z");

    private BidRepository bidRepository;
    private AuctionRepository auctionRepository;
    private UserRepository userRepository;
    private BidService service;

    private final User seller = user(1L, "Asha Seller");
    private final User rohan = user(2L, "Rohan Bidder");

    @BeforeEach
    void setUp() {
        bidRepository = mock(BidRepository.class);
        auctionRepository = mock(AuctionRepository.class);
        userRepository = mock(UserRepository.class);
        var clock = Clock.fixed(NOW, ZoneOffset.UTC);
        service = new BidService(bidRepository, auctionRepository, userRepository, new BidMapper(), clock);

        when(bidRepository.save(any(Bid.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void listsTheBidsOfAnAuction() {
        var auction = liveAuction(NOW.plus(1, ChronoUnit.HOURS));
        when(auctionRepository.existsById(5L)).thenReturn(true);
        when(bidRepository.findByAuctionIdOrderByPlacedAtDescIdDesc(5L))
                .thenReturn(List.of(new Bid(auction, rohan, new BigDecimal("1000.00"), NOW)));

        var bids = service.listBids(5L);

        assertThat(bids).singleElement().satisfies(bid -> {
            assertThat(bid.amount()).isEqualByComparingTo("1000.00");
            assertThat(bid.bidder().displayName()).isEqualTo("Rohan Bidder");
        });
    }

    @Test
    void rejectsListingTheBidsOfAnUnknownAuction() {
        when(auctionRepository.existsById(5L)).thenReturn(false);

        assertThatThrownBy(() -> service.listBids(5L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Auction 5");
    }

    @Nested
    class PlaceBid {

        @Test
        void savesTheBidWithTheServerTimeAndUpdatesTheAuction() {
            var auction = liveAuction(NOW.plus(1, ChronoUnit.HOURS));
            when(auctionRepository.findById(5L)).thenReturn(Optional.of(auction));
            when(userRepository.findById(2L)).thenReturn(Optional.of(rohan));

            var response = service.placeBid(5L, new PlaceBidRequest(2L, new BigDecimal("1000.00")));

            var saved = ArgumentCaptor.forClass(Bid.class);
            verify(bidRepository).save(saved.capture());
            assertThat(saved.getValue().getAmount()).isEqualByComparingTo("1000.00");
            assertThat(saved.getValue().getBidder()).isSameAs(rohan);
            assertThat(saved.getValue().getAuction()).isSameAs(auction);
            assertThat(saved.getValue().getPlacedAt()).isEqualTo(NOW);

            assertThat(response.amount()).isEqualByComparingTo("1000.00");
            assertThat(response.bidder().displayName()).isEqualTo("Rohan Bidder");
            assertThat(auction.getCurrentPrice()).isEqualByComparingTo("1000.00");
            assertThat(auction.getLeadingBidder()).isSameAs(rohan);
        }

        @Test
        void rejectsABidOnAnUnknownAuction() {
            when(auctionRepository.findById(5L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.placeBid(5L, new PlaceBidRequest(2L, new BigDecimal("1000.00"))))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Auction 5");
            verify(bidRepository, never()).save(any());
        }

        @Test
        void rejectsABidFromAnUnknownBidder() {
            when(auctionRepository.findById(5L)).thenReturn(Optional.of(liveAuction(NOW.plus(1, ChronoUnit.HOURS))));
            when(userRepository.findById(2L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.placeBid(5L, new PlaceBidRequest(2L, new BigDecimal("1000.00"))))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User 2");
            verify(bidRepository, never()).save(any());
        }

        @Test
        void doesNotSaveARejectedBid() {
            when(auctionRepository.findById(5L)).thenReturn(Optional.of(liveAuction(NOW.plus(1, ChronoUnit.HOURS))));
            when(userRepository.findById(2L)).thenReturn(Optional.of(rohan));

            assertThatThrownBy(() -> service.placeBid(5L, new PlaceBidRequest(2L, new BigDecimal("10.00"))))
                    .isInstanceOf(BusinessRuleException.class)
                    .extracting("code").isEqualTo("BID_TOO_LOW");
            verify(bidRepository, never()).save(any());
        }

        @Test
        void judgesLateBidsByTheServerClock() {
            when(auctionRepository.findById(5L)).thenReturn(Optional.of(liveAuction(NOW)));
            when(userRepository.findById(2L)).thenReturn(Optional.of(rohan));

            assertThatThrownBy(() -> service.placeBid(5L, new PlaceBidRequest(2L, new BigDecimal("1000.00"))))
                    .isInstanceOf(BusinessRuleException.class)
                    .extracting("code").isEqualTo("AUCTION_CLOSED");
            verify(bidRepository, never()).save(any());
        }
    }

    /** An ACTIVE auction sold by Asha: starting price 1000.00, increment 50.00. */
    private Auction liveAuction(Instant endTime) {
        var auction = new Auction(seller, null, "Vintage clock", "Brass, 1920s", null,
                new BigDecimal("1000.00"), new BigDecimal("50.00"), endTime.minus(1, ChronoUnit.DAYS), endTime);
        ReflectionTestUtils.setField(auction, "status", AuctionStatus.ACTIVE);
        return auction;
    }

    private static User user(Long id, String name) {
        var user = new User(name, name.toLowerCase().replace(' ', '.') + "@test.dev");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
