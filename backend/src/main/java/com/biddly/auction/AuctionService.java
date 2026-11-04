package com.biddly.auction;

import java.time.Clock;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.biddly.auction.dto.AuctionResponse;
import com.biddly.auction.dto.CreateAuctionRequest;
import com.biddly.category.CategoryRepository;
import com.biddly.common.PageResponse;
import com.biddly.common.ResourceNotFoundException;
import com.biddly.user.UserRepository;
import com.biddly.common.BusinessRuleException;

@Service
@Transactional(readOnly = true)
public class AuctionService {

    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final AuctionMapper auctionMapper;
    private final Clock clock;

    public AuctionService(AuctionRepository auctionRepository, UserRepository userRepository,
            CategoryRepository categoryRepository, AuctionMapper auctionMapper, Clock clock) {
        this.auctionRepository = auctionRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.auctionMapper = auctionMapper;
        this.clock = clock;
    }

    public PageResponse<AuctionResponse> listByStatus(AuctionStatus status, Pageable pageable) {
        var now = clock.instant();
        return PageResponse.from(
                auctionRepository.findByStatus(status, pageable),
                auction -> auctionMapper.toResponse(auction, now));
    }

    public AuctionResponse getById(Long id) {
        return auctionRepository.findWithDetailsById(id)
                .map(auction -> auctionMapper.toResponse(auction, clock.instant()))
                .orElseThrow(() -> new ResourceNotFoundException("Auction", id));
    }

    /**
     * CHALLENGE 2 - see docs/CHALLENGES.md for the full brief and hints.
     *
     * Acceptance criteria (the tests in AuctionServiceTest describe them exactly):
     *   1. startTime must not be in the past              -> BusinessRuleException("START_TIME_IN_PAST", ...)
     *   2. endTime must be after startTime                -> BusinessRuleException("INVALID_TIME_WINDOW", ...)
     *   3. the seller must exist                          -> ResourceNotFoundException("User", sellerId)
     *   4. if categoryId is given, the category must exist -> ResourceNotFoundException("Category", categoryId)
     *   5. save a new DRAFT auction and return it as an AuctionResponse
     *
     * Remove the throw below, then remove @Disabled from AuctionServiceTest and make it pass.
     */
    @Transactional
    public AuctionResponse createAuction(CreateAuctionRequest request) {
        var startTime = request.startTime();
        var endTime = request.endTime();
        if (startTime.isBefore(clock.instant())) {
            throw new BusinessRuleException("START_TIME_IN_PAST","The auction can't start in the past.");
        }
        if (!endTime.isAfter(startTime)) {
            throw new BusinessRuleException("INVALID_TIME_WINDOW","endTime must be after startTime");
        }
        var seller = userRepository.findById(request.sellerId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.sellerId()));
        var category = request.categoryId() != null ? categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.categoryId()))
                : null;
        var auction = auctionRepository.save(
            new Auction(seller, category, request.title(), request.description(), request.coverImageUrl(), request.startingPrice(), request.minimumIncrement(), startTime, endTime));
        return auctionMapper.toResponse(auction, clock.instant());
    }
}
