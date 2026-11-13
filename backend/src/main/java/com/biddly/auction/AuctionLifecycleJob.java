package com.biddly.auction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * The clock that moves auctions along: every few seconds, start what is due and close what has ended (R22).
 * All the logic is in {@link AuctionLifecycleService}, so it can be tested without waiting for a timer.
 */
@Component
@ConditionalOnProperty(name = "biddly.scheduler.enabled", havingValue = "true", matchIfMissing = true)
class AuctionLifecycleJob {

    private static final Logger log = LoggerFactory.getLogger(AuctionLifecycleJob.class);

    private final AuctionLifecycleService lifecycleService;
    private boolean warnedNotImplemented;

    AuctionLifecycleJob(AuctionLifecycleService lifecycleService) {
        this.lifecycleService = lifecycleService;
    }

    /** fixedDelay waits for one run to finish before timing the next, so runs never overlap. */
    @Scheduled(fixedDelayString = "${biddly.scheduler.interval-ms:5000}", initialDelayString = "${biddly.scheduler.interval-ms:5000}")
    void run() {
        try {
            var result = lifecycleService.advance();
            if (result.started() > 0 || result.completed() > 0) {
                log.info("Auction lifecycle: started {}, completed {}", result.started(), result.completed());
            }
        } catch (UnsupportedOperationException ex) {
            if (!warnedNotImplemented) {
                log.warn("Auctions won't start or close on time yet: {}", ex.getMessage());
                warnedNotImplemented = true;
            }
        } catch (RuntimeException ex) {
            log.error("Auction lifecycle run failed; the next run will try again", ex);
        }
    }
}
