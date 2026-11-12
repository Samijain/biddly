package com.biddly.bid;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
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
 * Fires real HTTP bids at the same auction at the same moment, against a real PostgreSQL.
 *
 * Races are normally a matter of luck. To make one happen every time, the server's clock pauses for
 * {@link #CLOCK_PAUSE_MS} when asked for "now". placeBid reads the clock after loading the auction, so every
 * request has loaded the same version of the auction before any of them can commit.
 */
@Import({ TestcontainersConfiguration.class, BidConcurrencyIntegrationTest.SlowClockConfig.class })
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class BidConcurrencyIntegrationTest {

    private static final int BIDDERS = 8;
    private static final long CLOCK_PAUSE_MS = 500;

    @TestConfiguration(proxyBeanMethods = false)
    static class SlowClockConfig {
        @Bean
        @Primary
        Clock slowClock() {
            return new Clock() {
                @Override
                public Instant instant() {
                    try {
                        Thread.sleep(CLOCK_PAUSE_MS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return Instant.now();
                }

                @Override
                public ZoneId getZone() {
                    return ZoneId.of("UTC");
                }

                @Override
                public Clock withZone(ZoneId zone) {
                    return this;
                }
            };
        }
    }

    private record Outcome(int status, String body) {
        boolean hasCode(String code) {
            return body != null && body.contains("\"code\":\"" + code + "\"");
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

    private Auction auction;
    private List<User> bidders;

    @BeforeEach
    void setUp() {
        bidRepository.deleteAll();
        auctionRepository.deleteAll();
        userRepository.deleteAll();
        var seller = userRepository.save(new User("Race Seller", "race.seller@test.dev"));
        bidders = IntStream.range(0, BIDDERS)
                .mapToObj(i -> userRepository.save(new User("Bidder " + i, "bidder" + i + "@test.dev")))
                .toList();
        var live = new Auction(seller, null, "Contested lamp", "Everyone wants it", null,
                new BigDecimal("1000.00"), new BigDecimal("50.00"),
                Instant.now().minus(1, ChronoUnit.HOURS), Instant.now().plus(1, ChronoUnit.DAYS));
        ReflectionTestUtils.setField(live, "status", AuctionStatus.ACTIVE);
        auction = auctionRepository.save(live);
    }

    /**
     * Passes already: @Version on Auction stops a second commit from overwriting the first, and the loser's
     * transaction rolls back with its bid. The data is safe. What the loser is *told* is another matter.
     */
    @Test
    void keepsThePriceAndTheHistoryInAgreement() throws Exception {
        var outcomes = race(i -> new BigDecimal(1000 + i * 100));

        var accepted = outcomes.stream().filter(o -> o.status() == 201).count();
        var stored = bidRepository.findByAuctionIdOrderByPlacedAtDescIdDesc(auction.getId());
        var reloaded = auctionRepository.findWithDetailsById(auction.getId()).orElseThrow();

        assertThat(accepted).isPositive();
        assertThat(stored).hasSize((int) accepted);
        var highest = stored.stream().map(Bid::getAmount).max(BigDecimal::compareTo).orElseThrow();
        assertThat(reloaded.getCurrentPrice()).isEqualByComparingTo(highest);
        var highestBid = stored.stream().filter(b -> b.getAmount().compareTo(highest) == 0).findFirst().orElseThrow();
        assertThat(reloaded.getLeadingBidder().getId()).isEqualTo(highestBid.getBidder().getId());
    }

    /** Losers get 409 CONCURRENT_UPDATE (optimistic) or 422 BID_TOO_LOW (they saw the winner's price), never a 5xx. */
    @Test
    void tellsTheLosersWhatHappenedInsteadOfFailing() throws Exception {
        var outcomes = race(i -> new BigDecimal("1050.00"));

        assertThat(outcomes).filteredOn(o -> o.status() == 201).hasSize(1);
        assertThat(outcomes).noneMatch(o -> o.status() >= 500);
        assertThat(outcomes).filteredOn(o -> o.status() != 201)
                .allMatch(o -> (o.status() == 409 && o.hasCode("CONCURRENT_UPDATE")) || (o.status() == 422 && o.hasCode("BID_TOO_LOW")));
        assertThat(bidRepository.findByAuctionIdOrderByPlacedAtDescIdDesc(auction.getId())).hasSize(1);
    }

    /** SELECT ... FOR UPDATE makes bids queue up, so each one is judged against the previous winner's price. */
    @Test
    void aPessimisticLockMakesBiddersWaitTheirTurn() throws Exception {
        var outcomes = race(i -> new BigDecimal("1050.00"));

        assertThat(outcomes).filteredOn(o -> o.status() == 201).hasSize(1);
        assertThat(outcomes).filteredOn(o -> o.status() == 422 && o.hasCode("BID_TOO_LOW")).hasSize(BIDDERS - 1);
        assertThat(outcomes).noneMatch(o -> o.status() == 409);
    }

    /** Every bidder sends one bid; all requests are released at the same instant. */
    private List<Outcome> race(IntFunction<BigDecimal> amountFor) throws Exception {
        var client = RestClient.create("http://localhost:" + port);
        var startingGun = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(BIDDERS)) {
            List<Future<Outcome>> futures = new ArrayList<>();
            for (int i = 0; i < BIDDERS; i++) {
                var body = Map.of("bidderId", bidders.get(i).getId(), "amount", amountFor.apply(i));
                futures.add(pool.submit(() -> {
                    startingGun.await();
                    return client.post()
                            .uri("/api/v1/auctions/{id}/bids", auction.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(body)
                            .exchange((request, response) -> new Outcome(response.getStatusCode().value(), response.bodyTo(String.class)));
                }));
            }
            startingGun.countDown();
            List<Outcome> outcomes = new ArrayList<>();
            for (var future : futures) {
                outcomes.add(future.get(30, TimeUnit.SECONDS));
            }
            return outcomes;
        }
    }
}
