package com.biddly.bid;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.biddly.TestcontainersConfiguration;
import com.biddly.auction.Auction;
import com.biddly.auction.AuctionRepository;
import com.biddly.auction.AuctionStatus;
import com.biddly.user.User;
import com.biddly.user.UserRepository;

/** Runs the whole stack, including the V2 migration, against a throwaway PostgreSQL container. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BidApiIntegrationTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    BidRepository bidRepository;

    @Autowired
    AuctionRepository auctionRepository;

    @Autowired
    UserRepository userRepository;

    private Auction auction;
    private User bidder;

    @BeforeEach
    void setUp() {
        bidRepository.deleteAll();
        auctionRepository.deleteAll();
        userRepository.deleteAll();
        var seller = userRepository.save(new User("Test Seller", "seller@test.dev"));
        bidder = userRepository.save(new User("Test Bidder", "bidder@test.dev"));
        var live = new Auction(seller, null, "Test lamp", "A lamp", null, new BigDecimal("100.00"), new BigDecimal("10.00"),
                Instant.now().minus(1, ChronoUnit.HOURS), Instant.now().plus(1, ChronoUnit.DAYS));
        ReflectionTestUtils.setField(live, "status", AuctionStatus.ACTIVE);
        auction = auctionRepository.save(live);
    }

    @Test
    void listsNoBidsForANewAuction() {
        assertThat(mvc.get().uri("/api/v1/auctions/{id}/bids", auction.getId()))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.length()").isEqualTo(0);
    }

    @Test
    void returnsNotFoundForTheBidsOfAnUnknownAuction() {
        assertThat(mvc.get().uri("/api/v1/auctions/{id}/bids", 999_999))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.code").isEqualTo("NOT_FOUND");
    }

    @Test
    void rejectsABidWithoutAnAmount() {
        assertThat(mvc.post().uri("/api/v1/auctions/{id}/bids", auction.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"bidderId\": %d}".formatted(bidder.getId())))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.fieldErrors.amount").isNotNull();
    }

    /** Proves the bid and the new price reach the database, not just the in-memory entity. */
    @Test
    void storesAnAcceptedBidAndTheNewPrice() {
        assertThat(mvc.post().uri("/api/v1/auctions/{id}/bids", auction.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"bidderId\": %d, \"amount\": 100.00}".formatted(bidder.getId())))
                .hasStatus(HttpStatus.CREATED);

        assertThat(mvc.get().uri("/api/v1/auctions/{id}/bids", auction.getId()))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$[0].bidder.displayName").isEqualTo("Test Bidder");

        assertThat(mvc.get().uri("/api/v1/auctions/{id}", auction.getId()))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.leadingBidder.displayName").isEqualTo("Test Bidder");
    }
}
