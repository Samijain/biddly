package com.biddly.bid;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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

    /**
     * A repeated request with the same Idempotency-Key gets the original 201 and the original bid back,
     * so a client that never saw the first answer can safely retry. The header is optional for now.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    BidResponse place(@PathVariable Long auctionId, @Valid @RequestBody PlaceBidRequest request,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey) {
        return bidService.placeBid(auctionId, request, idempotencyKey);
    }
}
