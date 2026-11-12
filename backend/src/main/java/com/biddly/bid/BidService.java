package com.biddly.bid;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import com.biddly.auction.Auction;
import com.biddly.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.biddly.auction.AuctionRepository;
import com.biddly.bid.dto.BidResponse;
import com.biddly.bid.dto.PlaceBidRequest;
import com.biddly.common.ResourceNotFoundException;
import com.biddly.user.UserRepository;

@Service
@Transactional(readOnly = true)
public class BidService {

    private final BidRepository bidRepository;
    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;
    private final BidMapper bidMapper;
    private final Clock clock;

    public BidService(BidRepository bidRepository, AuctionRepository auctionRepository, UserRepository userRepository,
            BidMapper bidMapper, Clock clock) {
        this.bidRepository = bidRepository;
        this.auctionRepository = auctionRepository;
        this.userRepository = userRepository;
        this.bidMapper = bidMapper;
        this.clock = clock;
    }

    /** Bid history for one auction, newest first (US-04). */
    public List<BidResponse> listBids(Long auctionId) {
        if (!auctionRepository.existsById(auctionId)) {
            throw new ResourceNotFoundException("Auction", auctionId);
        }
        return bidRepository.findByAuctionIdOrderByPlacedAtDescIdDesc(auctionId).stream()
                .map(bidMapper::toResponse)
                .toList();
    }

    /**
     * Places a bid (US-20). The auction applies R3-R5; "now" comes from the server clock (R4).
     * A rejected bid throws before anything is saved.
     */
    @Transactional
    public BidResponse placeBid(Long auctionId, PlaceBidRequest request) {
      Auction auction = auctionRepository.findById(auctionId).orElseThrow(() -> new ResourceNotFoundException("Auction", auctionId));
      User bidder = userRepository.findById(request.bidderId()).orElseThrow(() -> new ResourceNotFoundException("User", request.bidderId()));
      Instant clockNow = clock.instant();
      auction.acceptBid(bidder,request.amount(),clockNow);
      Bid bid = bidRepository.save(new Bid( auction, bidder, request.amount(), clockNow));
      return bidMapper.toResponse(bid);
    }
}
