package com.biddly.auction;

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
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.biddly.TestcontainersConfiguration;
import com.biddly.bid.BidRepository;
import com.biddly.user.User;
import com.biddly.user.UserRepository;

/** Runs the whole stack against a throwaway PostgreSQL container (Docker must be running). */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuctionApiIntegrationTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    AuctionRepository auctionRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    BidRepository bidRepository;

    private Auction draft;

    @BeforeEach
    void setUp() {
        bidRepository.deleteAll();
        auctionRepository.deleteAll();
        userRepository.deleteAll();
        var seller = userRepository.save(new User("Test Seller", "seller@test.dev"));
        var start = Instant.now().plus(1, ChronoUnit.HOURS);
        draft = auctionRepository.save(new Auction(seller, null, "Test lamp", "A lamp", null,
                new BigDecimal("100.00"), new BigDecimal("10.00"), start, start.plus(1, ChronoUnit.DAYS)));
    }

    @Test
    void listsAuctionsFilteredByStatus() {
        assertThat(mvc.get().uri("/api/v1/auctions?status=DRAFT"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.content[0].title").isEqualTo("Test lamp");

        assertThat(mvc.get().uri("/api/v1/auctions?status=ACTIVE"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.totalElements").isEqualTo(0);
    }

    @Test
    void returnsAuctionDetailsWithCurrentPriceEqualToStartingPrice() {
        assertThat(mvc.get().uri("/api/v1/auctions/{id}", draft.getId()))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.currentPrice").isEqualTo(100.0);
    }

    /** R25: whole rupees only, for the starting price and the increment. */
    @Test
    void rejectsPricesWithPaise() {
        var start = Instant.now().plus(1, ChronoUnit.HOURS);
        var body = """
                {"sellerId": %d, "title": "Brass lamp", "startingPrice": 100.50, "minimumIncrement": 10.25,
                 "startTime": "%s", "endTime": "%s"}""".formatted(draft.getSeller().getId(), start, start.plus(1, ChronoUnit.DAYS));

        var result = mvc.post().uri("/api/v1/auctions").contentType(MediaType.APPLICATION_JSON).content(body).exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.fieldErrors.startingPrice").isEqualTo("must be in whole rupees");
        assertThat(result).bodyJson().extractingPath("$.fieldErrors.minimumIncrement").isEqualTo("must be in whole rupees");
    }

    @Test
    void returnsConsistentErrorBodyForUnknownAuction() {
        assertThat(mvc.get().uri("/api/v1/auctions/{id}", 999_999))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.code").isEqualTo("NOT_FOUND");
    }
}
