package com.biddly.bid.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record BidResponse(Long id, Long auctionId, BigDecimal amount, BidderSummary bidder, Instant placedAt) {

    public record BidderSummary(Long id, String displayName) {
    }
}
