package com.biddly.auction.dto;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body of PUT /api/v1/auctions/{id}. PUT replaces every editable field, so the client always sends
 * the whole draft, not only the fields that changed.
 *
 * There is no sellerId: who owns an auction is never something the client can change.
 */
public record UpdateAuctionRequest(
        Long categoryId,
        @NotBlank @Size(max = 120) String title,
        @Size(max = 5000) String description,
        @Size(max = 500) String coverImageUrl,
        @NotNull @DecimalMin("1.00") @Digits(integer = 10, fraction = 2) BigDecimal startingPrice,
        @NotNull @DecimalMin("1.00") @Digits(integer = 10, fraction = 2) BigDecimal minimumIncrement,
        @NotNull Instant startTime,
        @NotNull Instant endTime) {
}
