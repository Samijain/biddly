package com.biddly.auction;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuctionLifecycleService {

    /** How many auctions one run moved to ACTIVE and to COMPLETED. */
    public record LifecycleRun(int started, int completed) {
    }

    private final AuctionRepository auctionRepository;
    private final Clock clock;

    public AuctionLifecycleService(AuctionRepository auctionRepository, Clock clock) {
        this.auctionRepository = auctionRepository;
        this.clock = clock;
    }

    /**
     * CHALLENGE 9 - see docs/CHALLENGES.md.
     *   1. read "now" once from the clock
     *   2. auctionRepository.findByStatusAndStartTimeLessThanEqual(SCHEDULED, now) -> auction.start(now) for each
     *   3. auctionRepository.findByStatusAndEndTimeLessThanEqual(ACTIVE, now)      -> auction.complete(now) for each
     *   4. return new LifecycleRun(how many started, how many completed)
     * Start first, then complete: an auction whose whole window passed while it was SCHEDULED must end up COMPLETED in one run.
     */
    @Transactional
    public LifecycleRun advance() {
        Instant now = clock.instant();
        List<Auction> dueToStart = auctionRepository.findByStatusAndStartTimeLessThanEqual(AuctionStatus.SCHEDULED, now);
        for(Auction auction:dueToStart)
            auction.start(now);

        List<Auction> dueToComplete = auctionRepository.findByStatusAndEndTimeLessThanEqual(AuctionStatus.ACTIVE, now);
        for(Auction auction:dueToComplete)
            auction.complete(now);

        return new LifecycleRun(dueToStart.size(), dueToComplete.size());
    }
}
