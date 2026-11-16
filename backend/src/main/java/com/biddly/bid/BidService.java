package com.biddly.bid;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.biddly.auction.Auction;
import com.biddly.common.BusinessRuleException;
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
     *
     * CHALLENGE 10 - see docs/CHALLENGES.md. Make a repeated request harmless (R23):
     *   - idempotencyKey is null                      -> behave exactly as today; don't look anything up
     *   - this bidder already placed a bid with this key:
     *       - same auction and same amount            -> return that bid, unchanged; place nothing new
     *       - different auction or amount             -> BusinessRuleException("IDEMPOTENCY_KEY_REUSED", ...)
     *   - otherwise                                   -> place the bid as today, and store the key with it
     * Where you put the lookup matters. BidIdempotencyIntegrationTest's race test will tell you if it's in the wrong place.
     */
    @Transactional
    public BidResponse placeBid(Long auctionId, PlaceBidRequest request, UUID idempotencyKey) {
      Auction auction = auctionRepository.findByIdForUpdate(auctionId).orElseThrow(() -> new ResourceNotFoundException("Auction", auctionId));
      if(idempotencyKey != null) {
          Optional<Bid> earlierBid = bidRepository.findByBidderIdAndIdempotencyKey(request.bidderId(), idempotencyKey);
          if (earlierBid.isPresent()) {
              Bid bid = earlierBid.get();
              if (auctionId.compareTo(bid.getAuction().getId()) == 0 && request.amount().compareTo(bid.getAmount()) == 0)
                  return bidMapper.toResponse(bid);
              else
                  throw new BusinessRuleException("IDEMPOTENCY_KEY_REUSED","This request key was already used for a different bid.");
          }
      }
      User bidder = userRepository.findById(request.bidderId()).orElseThrow(() -> new ResourceNotFoundException("User", request.bidderId()));
      Instant clockNow = clock.instant();
      auction.acceptBid(bidder,request.amount(),clockNow);
      Bid bid = bidRepository.save(new Bid( auction, bidder, request.amount(), clockNow, idempotencyKey));
      return bidMapper.toResponse(bid);
    }
}
