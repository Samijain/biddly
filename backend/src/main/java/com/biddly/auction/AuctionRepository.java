package com.biddly.auction;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * {@code @EntityGraph} loads seller and category in the same SQL query.
 * Without it, listing 20 auctions would run 1 + 20 + 20 queries (the "N+1 problem").
 */
public interface AuctionRepository extends JpaRepository<Auction, Long> {

    @EntityGraph(attributePaths = {"seller", "category", "leadingBidder"})
    Page<Auction> findByStatus(AuctionStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"seller", "category", "leadingBidder"})
    Optional<Auction> findWithDetailsById(Long id);

    /** Auctions in {@code status} whose start time has arrived, for the lifecycle job. */
    List<Auction> findByStatusAndStartTimeLessThanEqual(AuctionStatus status, Instant now);

    /** Auctions in {@code status} whose end time has arrived, for the lifecycle job. */
    List<Auction> findByStatusAndEndTimeLessThanEqual(AuctionStatus status, Instant now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Auction a WHERE a.id = :id")
    Optional<Auction> findByIdForUpdate(@Param("id") Long id);
}
