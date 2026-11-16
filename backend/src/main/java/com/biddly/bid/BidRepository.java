package com.biddly.bid;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BidRepository extends JpaRepository<Bid, Long> {

    /** Newest first (US-04). The bidder is fetched in the same query to avoid N+1. */
    @EntityGraph(attributePaths = "bidder")
    List<Bid> findByAuctionIdOrderByPlacedAtDescIdDesc(Long auctionId);

    /** The bid an earlier request with this key placed, if any (Challenge 10). Keys are unique per bidder. */
    Optional<Bid> findByBidderIdAndIdempotencyKey(Long bidderId, UUID idempotencyKey);
}
