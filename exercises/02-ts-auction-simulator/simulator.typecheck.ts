/**
 * Compile-time tests. This file is never run; `npm run typecheck` checks it.
 *
 * Each `@ts-expect-error` line is something your types must FORBID. If a marked line compiles
 * without an error, TypeScript reports "Unused '@ts-expect-error' directive", which means
 * your types are too loose.
 */
import { closeAuction, createAuction, getWinner, placeBid } from './simulator.ts'
import type { AuctionStatus, BidRejectionCode, OpenAuction } from './types.ts'

export function typeTests(open: OpenAuction): void {
  const closed = closeAuction(open)

  // These must compile:
  const winner: string | null = getWinner(closed)
  const next: OpenAuction = placeBid(open, 'rohan', 2000)

  // @ts-expect-error a closed auction cannot receive bids
  placeBid(closed, 'rohan', 2000)

  // @ts-expect-error an open auction has no winner yet
  getWinner(open)

  // @ts-expect-error only the two real statuses exist
  const typo: AuctionStatus = 'OPENED'

  // @ts-expect-error only the three real rejection codes exist
  const code: BidRejectionCode = 'TOO_LOW'

  // @ts-expect-error auctions are immutable
  open.currentPrice = 1

  // @ts-expect-error bid history cannot be changed directly
  open.bids.push(next.bids[0]!)

  // @ts-expect-error amounts are numbers, not strings
  placeBid(open, 'rohan', '2000')

  // @ts-expect-error minimumIncrement is required
  createAuction({ title: 'x', sellerId: 's', startingPrice: 10 })

  void [winner, typo, code]
}
