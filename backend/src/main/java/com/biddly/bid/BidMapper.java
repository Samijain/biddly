package com.biddly.bid;

import org.springframework.stereotype.Component;

import com.biddly.bid.dto.BidResponse;
import com.biddly.bid.dto.BidResponse.BidderSummary;

@Component
public class BidMapper {

    public BidResponse toResponse(Bid bid) {
        var bidder = bid.getBidder();
        return new BidResponse(
                bid.getId(),
                bid.getAuction().getId(),
                bid.getAmount(),
                new BidderSummary(bidder.getId(), bidder.getDisplayName()),
                bid.getPlacedAt());
    }
}
