import type { Auction } from '../auctions/types'

/** Mirrors R5 for the hint text only. The server decides whether a bid is high enough. */
export function minimumNextBid(auction: Pick<Auction, 'leadingBidder' | 'currentPrice' | 'startingPrice' | 'minimumIncrement'>): number {
  return auction.leadingBidder ? auction.currentPrice + auction.minimumIncrement : auction.startingPrice
}
