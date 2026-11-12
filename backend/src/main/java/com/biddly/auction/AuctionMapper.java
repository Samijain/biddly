package com.biddly.auction;

import java.time.Instant;

import org.springframework.stereotype.Component;

import com.biddly.auction.dto.AuctionResponse;
import com.biddly.auction.dto.AuctionResponse.BidderSummary;
import com.biddly.auction.dto.AuctionResponse.CategorySummary;
import com.biddly.auction.dto.AuctionResponse.SellerSummary;

@Component
public class AuctionMapper {

    /**
     * serverTime lets the browser correct for a wrong local clock when it renders the countdown.
     */
    public AuctionResponse toResponse(Auction auction, Instant serverTime) {
        var category = auction.getCategory();
        var seller = auction.getSeller();
        var leadingBidder = auction.getLeadingBidder();
        return new AuctionResponse(
                auction.getId(),
                auction.getTitle(),
                auction.getDescription(),
                auction.getCoverImageUrl(),
                category == null ? null : new CategorySummary(category.getId(), category.getName(), category.getSlug()),
                new SellerSummary(seller.getId(), seller.getDisplayName()),
                auction.getStartingPrice(),
                auction.getCurrentPrice(),
                auction.getMinimumIncrement(),
                leadingBidder == null ? null : new BidderSummary(leadingBidder.getId(), leadingBidder.getDisplayName()),
                auction.getStartTime(),
                auction.getEndTime(),
                auction.getStatus(),
                serverTime);
    }
}
