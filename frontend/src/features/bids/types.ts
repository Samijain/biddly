import type { SellerSummary } from '../auctions/types'

/** Mirrors com.biddly.bid.dto.BidResponse. Keep the two in sync. */
export interface Bid {
  id: number
  auctionId: number
  amount: number
  bidder: SellerSummary
  placedAt: string
}

export interface PlaceBidInput {
  bidderId: number
  amount: number
}
