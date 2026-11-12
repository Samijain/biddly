package com.biddly.auction.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.biddly.auction.AuctionStatus;

/**
 * What the API returns. Never return the JPA entity itself: it would expose internal fields,
 * trigger lazy-loading outside a transaction, and tie the API contract to the table layout.
 */
public record AuctionResponse(
        Long id,
        String title,
        String description,
        String coverImageUrl,
        CategorySummary category,
        SellerSummary seller,
        BigDecimal startingPrice,
        BigDecimal currentPrice,
        BigDecimal minimumIncrement,
        /** null while there are no bids. */
        BidderSummary leadingBidder,
        Instant startTime,
        Instant endTime,
        AuctionStatus status,
        Instant serverTime) {

    public record CategorySummary(Long id, String name, String slug) {
    }

    public record SellerSummary(Long id, String displayName) {
    }

    public record BidderSummary(Long id, String displayName) {
    }
}
