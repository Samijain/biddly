package com.biddly.auction;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import com.biddly.TestcontainersConfiguration;
import com.biddly.bid.BidRepository;
import com.biddly.user.User;
import com.biddly.user.UserRepository;

/** One lifecycle run against a real PostgreSQL. The job itself is off in tests; the test calls the service directly. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles("test")
class AuctionLifecycleIntegrationTest {

    @Autowired
    ApplicationContext context;

    @Autowired
    AuctionLifecycleService lifecycleService;

    @Autowired
    AuctionRepository auctionRepository;

    @Autowired
    BidRepository bidRepository;

    @Autowired
    UserRepository userRepository;

    private User seller;
    private User bidder;
    private final Instant now = Instant.now();

    @BeforeEach
    void setUp() {
        bidRepository.deleteAll();
        auctionRepository.deleteAll();
        userRepository.deleteAll();
        seller = userRepository.save(new User("Lifecycle Seller", "lifecycle.seller@test.dev"));
        bidder = userRepository.save(new User("Lifecycle Bidder", "lifecycle.bidder@test.dev"));
    }

    @Test
    void doesNotRunTheBackgroundJobInTests() {
        assertThat(context.getBeansOfType(AuctionLifecycleJob.class)).isEmpty();
    }

    /** CHALLENGE 9: remove @Disabled once Auction.start, Auction.complete and advance() are implemented. */
    @Test
    void movesEveryAuctionToTheRightStateInOneRun() {
        var dueToStart = save(hours(-1), hours(24), AuctionStatus.SCHEDULED, null);
        var notYetDue = save(hours(1), hours(24), AuctionStatus.SCHEDULED, null);
        var windowPassedWhileScheduled = save(hours(-48), hours(-24), AuctionStatus.SCHEDULED, null);
        var stillRunning = save(hours(-1), hours(1), AuctionStatus.ACTIVE, null);
        var endedWithABid = save(hours(-24), hours(-1), AuctionStatus.ACTIVE, bidder);
        var endedWithoutBids = save(hours(-24), hours(-1), AuctionStatus.ACTIVE, null);
        var cancelled = save(hours(-24), hours(-1), AuctionStatus.CANCELLED, null);

        var run = lifecycleService.advance();

        assertThat(statusOf(dueToStart)).isEqualTo(AuctionStatus.ACTIVE);
        assertThat(statusOf(notYetDue)).isEqualTo(AuctionStatus.SCHEDULED);
        assertThat(statusOf(windowPassedWhileScheduled)).isEqualTo(AuctionStatus.COMPLETED);
        assertThat(statusOf(stillRunning)).isEqualTo(AuctionStatus.ACTIVE);
        assertThat(statusOf(endedWithABid)).isEqualTo(AuctionStatus.COMPLETED);
        assertThat(statusOf(endedWithoutBids)).isEqualTo(AuctionStatus.COMPLETED);
        assertThat(statusOf(cancelled)).isEqualTo(AuctionStatus.CANCELLED);

        var winner = auctionRepository.findWithDetailsById(endedWithABid.getId()).orElseThrow().getLeadingBidder();
        assertThat(winner.getId()).isEqualTo(bidder.getId());
        assertThat(auctionRepository.findWithDetailsById(endedWithoutBids.getId()).orElseThrow().getLeadingBidder()).isNull();

        // Started: dueToStart and windowPassedWhileScheduled. Completed: windowPassedWhileScheduled and the two ended ones.
        assertThat(run).isEqualTo(new AuctionLifecycleService.LifecycleRun(2, 3));
    }

    private Auction save(Instant start, Instant end, AuctionStatus status, User leader) {
        var auction = new Auction(seller, null, "Lifecycle item", null, null,
                new BigDecimal("100.00"), new BigDecimal("10.00"), start, end);
        ReflectionTestUtils.setField(auction, "status", status);
        ReflectionTestUtils.setField(auction, "leadingBidder", leader);
        return auctionRepository.save(auction);
    }

    private AuctionStatus statusOf(Auction auction) {
        return auctionRepository.findById(auction.getId()).orElseThrow().getStatus();
    }

    private Instant hours(long amount) {
        return now.plus(amount, ChronoUnit.HOURS);
    }
}
