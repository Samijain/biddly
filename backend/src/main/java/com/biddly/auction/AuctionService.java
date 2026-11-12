package com.biddly.auction;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import com.biddly.category.Category;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.biddly.auction.dto.AuctionResponse;
import com.biddly.auction.dto.CreateAuctionRequest;
import com.biddly.auction.dto.UpdateAuctionRequest;
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
     * Creates a draft auction (rules R13, R14):
     *   1. startTime must not be in the past              -> BusinessRuleException("START_TIME_IN_PAST", ...)
     *   2. endTime must be after startTime                -> BusinessRuleException("INVALID_TIME_WINDOW", ...)
     *   3. the seller must exist                          -> ResourceNotFoundException("User", sellerId)
     *   4. if categoryId is given, the category must exist -> ResourceNotFoundException("Category", categoryId)
     *   5. save a new DRAFT auction and return it as an AuctionResponse
     */
    @Transactional
    public AuctionResponse createAuction(CreateAuctionRequest request) {
        var now = clock.instant();
        validateTimeWindow(request.startTime(), request.endTime(), now);
        var category = findCategory(request.categoryId());
        var seller = userRepository.findById(request.sellerId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.sellerId()));
        var auction = auctionRepository.save(
            new Auction(seller, category, request.title(), request.description(), request.coverImageUrl(), request.startingPrice(), request.minimumIncrement(), request.startTime(), request.endTime()));
        return auctionMapper.toResponse(auction, now);
    }

    /**
     * Publishes an auction (rules R15, R16 live on {@link Auction#publish}).
     *   1. load it with auctionRepository.findById(id); unknown id -> ResourceNotFoundException("Auction", id)
     *   2. let the entity apply the rules: auction.publish(now)
     *   3. return the auction as an AuctionResponse
     */
    @Transactional
    public AuctionResponse publishAuction(Long id) {
        Auction auction = auctionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Auction", id));
        Instant clockNow = clock.instant();
        auction.publish(clockNow);
        return auctionMapper.toResponse(auction, clockNow);
    }

    /**
     * Cancels an auction (rule R17 lives on {@link Auction#cancel}).
     *   1. load it with auctionRepository.findById(id); unknown id -> ResourceNotFoundException("Auction", id)
     *   2. let the entity apply the rules: auction.cancel()
     *   3. return the auction as an AuctionResponse
     */
    @Transactional
    public AuctionResponse cancelAuction(Long id) {
        Auction auction= auctionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Auction", id));
        auction.cancel();
        return auctionMapper.toResponse(auction, clock.instant());
    }

    /**
     * Replaces the details of a draft (rules R14, R18; the status rule lives on {@link Auction#updateDetails}).
     *   1. load it with auctionRepository.findById(id); unknown id -> ResourceNotFoundException("Auction", id)
     *   2. the same time-window rules as createAuction (R14): START_TIME_IN_PAST, INVALID_TIME_WINDOW
     *   3. if categoryId is given, the category must exist -> ResourceNotFoundException("Category", categoryId)
     *   4. let the entity apply its rules: auction.updateDetails(...)
     *   5. return the auction as an AuctionResponse
     */
    @Transactional
    public AuctionResponse updateAuction(Long id, UpdateAuctionRequest request) {
        Auction auction = auctionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Auction", id));

        var now = clock.instant();
        validateTimeWindow(request.startTime(), request.endTime(), now);
        var category = findCategory(request.categoryId());

        auction.updateDetails(category,request.title(),request.description(),request.coverImageUrl(),
                request.startingPrice(),request.minimumIncrement(),request.startTime(),request.endTime());

        return auctionMapper.toResponse(auction,now);
    }

    private Category findCategory(Long categoryId) {
        return  categoryId != null ? categoryRepository.findById(categoryId)
                                                      .orElseThrow(() -> new ResourceNotFoundException("Category", categoryId))
                : null;
    }

    private void validateTimeWindow(Instant startTime, Instant endTime, Instant now){
        if (startTime.isBefore(now)) {
            throw new BusinessRuleException("START_TIME_IN_PAST","The auction can't start in the past.");
        }
        if (!endTime.isAfter(startTime)) {
            throw new BusinessRuleException("INVALID_TIME_WINDOW","endTime must be after startTime");
        }
    }
}
