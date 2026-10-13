package com.biddly.auction;

/**
 * Auction lifecycle:
 * <pre>
 *   DRAFT -> SCHEDULED -> ACTIVE -> COMPLETED
 *                           |
 *                           -> CANCELLED
 * </pre>
 * Allowed transitions are listed in docs/business-rules.md.
 */
public enum AuctionStatus {
    DRAFT,
    SCHEDULED,
    ACTIVE,
    COMPLETED,
    CANCELLED
}
