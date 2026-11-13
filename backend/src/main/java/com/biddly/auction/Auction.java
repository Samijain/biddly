package com.biddly.auction;

import java.math.BigDecimal;
import java.time.Instant;

import com.biddly.common.BusinessRuleException;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.biddly.category.Category;
import com.biddly.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "auctions")
public class Auction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(length = 500)
    private String coverImageUrl;

    /** Money is always BigDecimal / NUMERIC. Never double: 0.1 + 0.2 != 0.3 in floating point. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal startingPrice;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal currentPrice;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal minimumIncrement;

    /** Who placed the current highest bid; null while there are no bids. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leading_bidder_id")
    private User leadingBidder;

    @Column(nullable = false)
    private Instant startTime;

    @Column(nullable = false)
    private Instant endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuctionStatus status;

    /** Optimistic-locking counter. You will rely on it in Milestone 6 (concurrent bids). */
    @Version
    private long version;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    protected Auction() {
    }

    /** Creates a new auction in DRAFT status whose current price starts at the starting price. */
    public Auction(User seller, Category category, String title, String description, String coverImageUrl,
            BigDecimal startingPrice, BigDecimal minimumIncrement, Instant startTime, Instant endTime) {
        this.seller = seller;
        this.category = category;
        this.title = title;
        this.description = description;
        this.coverImageUrl = coverImageUrl;
        this.startingPrice = startingPrice;
        this.currentPrice = startingPrice;
        this.minimumIncrement = minimumIncrement;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = AuctionStatus.DRAFT;
    }

    /**
     * Publishes a draft so bidders can see it. "now" is passed in because an entity is not a Spring bean
     * and cannot have the Clock injected.
     *   1. status is not DRAFT          -> BusinessRuleException("AUCTION_NOT_DRAFT", ...)
     *   2. endTime is not after now     -> BusinessRuleException("AUCTION_ALREADY_ENDED", ...)
     *   3. startTime is not after now   -> status becomes ACTIVE
     *      otherwise                    -> status becomes SCHEDULED
     */
    public void publish(Instant now) {
        if(!status.equals(AuctionStatus.DRAFT))
            throw new BusinessRuleException("AUCTION_NOT_DRAFT","Auction is not in draft status");
        if(!endTime.isAfter(now))
            throw new BusinessRuleException("AUCTION_ALREADY_ENDED","Auction end time is not after now");

        status= !startTime.isAfter(now) ? AuctionStatus.ACTIVE : AuctionStatus.SCHEDULED;
    }

    /**
     * Cancels an auction that has not finished.
     *   1. status is COMPLETED or CANCELLED -> BusinessRuleException("AUCTION_NOT_CANCELLABLE", ...)  (R17)
     *   2. it already has a bid             -> BusinessRuleException("AUCTION_HAS_BIDS", ...)         (R10)
     *   3. otherwise                        -> status becomes CANCELLED
     */
    public void cancel() {
       if(status.equals(AuctionStatus.CANCELLED)||status.equals(AuctionStatus.COMPLETED)){
           throw new BusinessRuleException("AUCTION_NOT_CANCELLABLE","Auction is already cancelled or completed");
       }
       if(leadingBidder!=null)
           throw new BusinessRuleException("AUCTION_HAS_BIDS","Bidding is already started cannot cancel the Auction");
       status=AuctionStatus.CANCELLED;
    }

    /**
     * CHALLENGE 9 - see docs/CHALLENGES.md. Called by the lifecycle job when the start time arrives (R22).
     *   1. status is not SCHEDULED       -> BusinessRuleException("AUCTION_NOT_SCHEDULED", ...)
     *   2. startTime is after now        -> BusinessRuleException("START_TIME_NOT_REACHED", ...)
     *   3. otherwise                     -> status becomes ACTIVE
     */
    public void start(Instant now) {
        if(status != AuctionStatus.SCHEDULED)
            throw new BusinessRuleException("AUCTION_NOT_SCHEDULED","Only a scheduled auction can be started");
        if(startTime.isAfter(now))
            throw new BusinessRuleException("START_TIME_NOT_REACHED","Auctions start time is not yet reached");
        status = AuctionStatus.ACTIVE;
    }

    /**
     * CHALLENGE 9 - see docs/CHALLENGES.md. Called by the lifecycle job when the end time arrives (R7, R8, R22).
     *   1. status is not ACTIVE          -> BusinessRuleException("AUCTION_NOT_ACTIVE", ...)
     *   2. now is before endTime         -> BusinessRuleException("END_TIME_NOT_REACHED", ...)
     *   3. otherwise                     -> status becomes COMPLETED
     * The leading bidder at that moment is the winner (R7); no leader means no winner (R8). Don't change leadingBidder.
     */
    public void complete(Instant now) {
        if(status != AuctionStatus.ACTIVE)
            throw new BusinessRuleException("AUCTION_NOT_ACTIVE","Only a active auction can be completed");
        if(now.isBefore(endTime))
            throw new BusinessRuleException("END_TIME_NOT_REACHED","Auctions end time is not yet reached");
        status = AuctionStatus.COMPLETED;
    }

    /**
     * Replaces the editable details of a draft (rules R2, R18). The service has already checked the time
     * window and looked up the category.
     *   1. status is not DRAFT  -> BusinessRuleException("AUCTION_NOT_EDITABLE", ...), nothing changes
     *   2. otherwise            -> copy every argument into its field, and set currentPrice to the new
     *                              startingPrice (a draft has no bids yet)
     */
    public void updateDetails(Category category, String title, String description, String coverImageUrl,
            BigDecimal startingPrice, BigDecimal minimumIncrement, Instant startTime, Instant endTime) {
        if(!status.equals(AuctionStatus.DRAFT))
            throw new BusinessRuleException("AUCTION_NOT_EDITABLE","Auction is not editable");

        this.category = category;
        this.title = title;
        this.description = description;
        this.coverImageUrl = coverImageUrl;
        this.startingPrice = startingPrice;
        this.minimumIncrement = minimumIncrement;
        this.currentPrice = startingPrice;
        this.startTime = startTime;
        this.endTime = endTime;

    }

    /**
     * Accepts a bid if it follows the rules, checked in this order, and throws before changing anything:
     *   1. status is not ACTIVE                -> BusinessRuleException("AUCTION_NOT_ACTIVE", ...)
     *   2. now is not before endTime (R4)      -> BusinessRuleException("AUCTION_CLOSED", ...)
     *   3. the bidder is the seller (R3)       -> BusinessRuleException("SELF_BIDDING_NOT_ALLOWED", ...)
     *   4. amount is below the minimum (R5)    -> BusinessRuleException("BID_TOO_LOW", ...)
     *        no bids yet:  minimum = startingPrice
     *        otherwise:    minimum = currentPrice + minimumIncrement
     *   5. otherwise -> currentPrice becomes amount, leadingBidder becomes bidder
     *
     * Saving the Bid row is the service's job; this method only guards and updates the auction.
     */
    public void acceptBid(User bidder, BigDecimal amount, Instant now) {
        if(!status.equals(AuctionStatus.ACTIVE))
            throw new BusinessRuleException("AUCTION_NOT_ACTIVE","Auction is not active yet.");
        if(!endTime.isAfter(now))
            throw new BusinessRuleException("AUCTION_CLOSED","Auction is closed");
        if(bidder.getId().compareTo(seller.getId()) == 0)
            throw new BusinessRuleException("SELF_BIDDING_NOT_ALLOWED","Self bidding is not allowed");
        BigDecimal min = leadingBidder == null ? startingPrice : currentPrice.add(minimumIncrement);
        if(amount.compareTo(min) < 0) {
            throw new BusinessRuleException("BID_TOO_LOW", "Your bid must be at least ₹" + min);
        }

        currentPrice = amount;
        leadingBidder = bidder;
    }

    public Long getId() {
        return id;
    }

    public User getSeller() {
        return seller;
    }

    public Category getCategory() {
        return category;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public BigDecimal getStartingPrice() {
        return startingPrice;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public BigDecimal getMinimumIncrement() {
        return minimumIncrement;
    }

    public User getLeadingBidder() {
        return leadingBidder;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
