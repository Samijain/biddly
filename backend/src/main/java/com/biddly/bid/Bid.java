package com.biddly.bid;

import java.math.BigDecimal;
import java.time.Instant;

import com.biddly.auction.Auction;
import com.biddly.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** An accepted bid. Immutable (R6): every field is set once in the constructor and there are no setters. */
@Entity
@Table(name = "bids")
public class Bid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auction_id", nullable = false, updatable = false)
    private Auction auction;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bidder_id", nullable = false, updatable = false)
    private User bidder;

    @Column(nullable = false, updatable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    /** Set from the server's Clock, never from the client (R4). */
    @Column(nullable = false, updatable = false)
    private Instant placedAt;

    protected Bid() {
    }

    public Bid(Auction auction, User bidder, BigDecimal amount, Instant placedAt) {
        this.auction = auction;
        this.bidder = bidder;
        this.amount = amount;
        this.placedAt = placedAt;
    }

    public Long getId() {
        return id;
    }

    public Auction getAuction() {
        return auction;
    }

    public User getBidder() {
        return bidder;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Instant getPlacedAt() {
        return placedAt;
    }
}
