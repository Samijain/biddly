package com.biddly.auction;

import java.net.URI;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.biddly.auction.dto.AuctionResponse;
import com.biddly.auction.dto.CreateAuctionRequest;
import com.biddly.common.PageResponse;

import jakarta.validation.Valid;

/** Controllers translate HTTP to service calls. Business rules never live here. */
@RestController
@RequestMapping("/api/v1/auctions")
public class AuctionController {

    private final AuctionService auctionService;

    public AuctionController(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @GetMapping
    PageResponse<AuctionResponse> list(
            @RequestParam(defaultValue = "ACTIVE") AuctionStatus status,
            @PageableDefault(size = 20, sort = "endTime", direction = Sort.Direction.ASC) Pageable pageable) {
        return auctionService.listByStatus(status, pageable);
    }

    @GetMapping("/{auctionId}")
    AuctionResponse get(@PathVariable Long auctionId) {
        return auctionService.getById(auctionId);
    }

    @PostMapping
    ResponseEntity<AuctionResponse> create(@Valid @RequestBody CreateAuctionRequest request) {
        var created = auctionService.createAuction(request);
        return ResponseEntity.created(URI.create("/api/v1/auctions/" + created.id())).body(created);
    }
}
