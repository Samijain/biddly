package com.biddly.auction;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * {@code @EntityGraph} loads seller and category in the same SQL query.
 * Without it, listing 20 auctions would run 1 + 20 + 20 queries (the "N+1 problem").
 */
public interface AuctionRepository extends JpaRepository<Auction, Long> {

    @EntityGraph(attributePaths = {"seller", "category"})
    Page<Auction> findByStatus(AuctionStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"seller", "category"})
    Optional<Auction> findWithDetailsById(Long id);
}
