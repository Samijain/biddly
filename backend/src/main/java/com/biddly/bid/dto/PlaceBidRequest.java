package com.biddly.bid.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

/**
 * Body of POST /api/v1/auctions/{auctionId}/bids. Bean Validation checks the shape only; whether the amount
 * is high enough depends on the auction, so that rule lives in Auction.acceptBid.
 *
 * bidderId is temporary, like sellerId in CreateAuctionRequest: from Milestone 5 the bidder is the signed-in user.
 */
public record PlaceBidRequest(
        @NotNull Long bidderId,
        @NotNull @DecimalMin("1.00") @Digits(integer = 10, fraction = 2) BigDecimal amount) {
}
