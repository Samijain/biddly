package com.biddly.bid;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.biddly.bid.dto.BidResponse;
import com.biddly.bid.dto.PlaceBidRequest;

import jakarta.validation.Valid;

/** Bids are a sub-resource of an auction: /api/v1/auctions/{auctionId}/bids. */
@RestController
@RequestMapping("/api/v1/auctions/{auctionId}/bids")
public class BidController {

    private final BidService bidService;

    public BidController(BidService bidService) {
        this.bidService = bidService;
    }

    @GetMapping
    List<BidResponse> list(@PathVariable Long auctionId) {
        return bidService.listBids(auctionId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    BidResponse place(@PathVariable Long auctionId, @Valid @RequestBody PlaceBidRequest request) {
        return bidService.placeBid(auctionId, request);
    }
}
