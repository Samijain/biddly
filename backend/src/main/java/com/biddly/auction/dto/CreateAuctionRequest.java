package com.biddly.auction.dto;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Bean Validation checks the shape of a single field ("is it present? is it positive?").
 * Rules that compare fields or need the database (end after start, seller exists) belong
 * in AuctionService.
 *
 * sellerId is temporary: in Milestone 5 it is taken from the signed-in user instead,
 * because a client must never be trusted to say who it is.
 */
public record CreateAuctionRequest(
        @NotNull Long sellerId,
        Long categoryId,
        @NotBlank @Size(max = 120) String title,
        @Size(max = 5000) String description,
        @Size(max = 500) String coverImageUrl,
        @NotNull @DecimalMin("1.00") @Digits(integer = 10, fraction = 2) BigDecimal startingPrice,
        @NotNull @DecimalMin("1.00") @Digits(integer = 10, fraction = 2) BigDecimal minimumIncrement,
        @NotNull Instant startTime,
        @NotNull Instant endTime) {
}
