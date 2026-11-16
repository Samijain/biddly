package com.biddly.bid;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import com.biddly.TestcontainersConfiguration;
import com.biddly.auction.Auction;
import com.biddly.auction.AuctionRepository;
import com.biddly.auction.AuctionStatus;
import com.biddly.user.User;
import com.biddly.user.UserRepository;

/**
 * Repeated "Place bid" requests over real HTTP against a real PostgreSQL (Challenge 10).
 * Uses the same slow clock as BidConcurrencyIntegrationTest, so the race test below is deterministic
 * and Spring can reuse that test's application context.
 */
@Import({ TestcontainersConfiguration.class, BidConcurrencyIntegrationTest.SlowClockConfig.class })
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class BidIdempotencyIntegrationTest {

    private static final int DOUBLE_CLICKS = 6;

    private record Outcome(int status, String body) {
        boolean hasCode(String code) {
            return body != null && body.contains("\"code\":\"" + code + "\"");
        }

        Long bidId() {
            var match = Pattern.compile("^\\{\"id\":(\\d+)").matcher(body);
            return match.find() ? Long.valueOf(match.group(1)) : null;
        }
    }

    @LocalServerPort
    int port;

    @Autowired
    BidRepository bidRepository;

    @Autowired
    AuctionRepository auctionRepository;

    @Autowired
    UserRepository userRepository;

    private RestClient client;
    private Auction auction;
    private User rohan;
    private User meera;

    @BeforeEach
    void setUp() {
        client = RestClient.create("http://localhost:" + port);
        bidRepository.deleteAll();
        auctionRepository.deleteAll();
        userRepository.deleteAll();
        var seller = userRepository.save(new User("Retry Seller", "retry.seller@test.dev"));
        rohan = userRepository.save(new User("Retry Rohan", "retry.rohan@test.dev"));
        meera = userRepository.save(new User("Retry Meera", "retry.meera@test.dev"));
        var live = new Auction(seller, null, "Flaky-wifi lamp", "Bid twice, pay once", null,
                new BigDecimal("1000.00"), new BigDecimal("50.00"),
                Instant.now().minus(1, ChronoUnit.HOURS), Instant.now().plus(1, ChronoUnit.DAYS));
        ReflectionTestUtils.setField(live, "status", AuctionStatus.ACTIVE);
        auction = auctionRepository.save(live);
    }

    // --- Pass already, and must keep passing once the key is used ---

    @Test
    void rejectsAKeyThatIsNotAUuid() {
        var outcome = bid(rohan, "1000.00", "not-a-uuid");

        assertThat(outcome.status()).isEqualTo(400);
        assertThat(outcome.hasCode("INVALID_PARAMETER")).isTrue();
        assertThat(storedBids()).isZero();
    }

    @Test
    void treatsBidsWithoutAKeyAsSeparateBids() {
        assertThat(bid(rohan, "1000.00", null).status()).isEqualTo(201);
        assertThat(bid(rohan, "1050.00", null).status()).isEqualTo(201);

        assertThat(storedBids()).isEqualTo(2);
    }

    @Test
    void treatsDifferentKeysAsDifferentBids() {
        assertThat(bid(rohan, "1000.00", UUID.randomUUID().toString()).status()).isEqualTo(201);
        assertThat(bid(rohan, "1050.00", UUID.randomUUID().toString()).status()).isEqualTo(201);

        assertThat(storedBids()).isEqualTo(2);
    }

    @Test
    void keepsKeysSeparateForEachBidder() {
        var sharedKey = UUID.randomUUID().toString();

        assertThat(bid(rohan, "1000.00", sharedKey).status()).isEqualTo(201);
        assertThat(bid(meera, "1050.00", sharedKey).status()).isEqualTo(201);

        assertThat(storedBids()).isEqualTo(2);
    }

    // --- CHALLENGE 10 ---

    @Test
    void answersARetryWithTheOriginalBid() {
        var key = UUID.randomUUID().toString();

        var first = bid(rohan, "1000.00", key);
        var retry = bid(rohan, "1000", key);

        assertThat(first.status()).isEqualTo(201);
        assertThat(retry.status()).isEqualTo(201);
        assertThat(retry.bidId()).isEqualTo(first.bidId());
        assertThat(storedBids()).isEqualTo(1);
        assertThat(auctionRepository.findById(auction.getId()).orElseThrow().getCurrentPrice()).isEqualByComparingTo("1000.00");
    }

    @Test
    void refusesToReuseAKeyForADifferentBid() {
        var key = UUID.randomUUID().toString();
        bid(rohan, "1000.00", key);

        var reused = bid(rohan, "1100.00", key);

        assertThat(reused.status()).isEqualTo(422);
        assertThat(reused.hasCode("IDEMPOTENCY_KEY_REUSED")).isTrue();
        assertThat(storedBids()).isEqualTo(1);
        assertThat(auctionRepository.findById(auction.getId()).orElseThrow().getCurrentPrice()).isEqualByComparingTo("1000.00");
    }

    /** Several copies of one request arrive together, like a double-click or a retry that overtakes the original. */
    @Test
    void storesOneBidWhenCopiesOfARequestArriveTogether() throws Exception {
        var key = UUID.randomUUID().toString();
        var startingGun = new CountDownLatch(1);
        List<Outcome> outcomes = new ArrayList<>();
        try (var pool = Executors.newFixedThreadPool(DOUBLE_CLICKS)) {
            List<Future<Outcome>> futures = new ArrayList<>();
            for (int i = 0; i < DOUBLE_CLICKS; i++) {
                futures.add(pool.submit(() -> {
                    startingGun.await();
                    return bid(rohan, "1000.00", key);
                }));
            }
            startingGun.countDown();
            for (var future : futures) {
                outcomes.add(future.get(30, TimeUnit.SECONDS));
            }
        }

        assertThat(outcomes).allMatch(o -> o.status() == 201);
        assertThat(outcomes).extracting(Outcome::bidId).containsOnly(outcomes.getFirst().bidId());
        assertThat(storedBids()).isEqualTo(1);
    }

    private Outcome bid(User bidder, String amount, String key) {
        var request = client.post()
                .uri("/api/v1/auctions/{id}/bids", auction.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("bidderId", bidder.getId(), "amount", new BigDecimal(amount)));
        if (key != null) {
            request = request.header("Idempotency-Key", key);
        }
        return request.exchange((req, response) -> new Outcome(response.getStatusCode().value(), response.bodyTo(String.class)));
    }

    private int storedBids() {
        return bidRepository.findByAuctionIdOrderByPlacedAtDescIdDesc(auction.getId()).size();
    }
}
