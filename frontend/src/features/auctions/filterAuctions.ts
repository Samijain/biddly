import type { Auction, AuctionFilter } from './types'

/**
 * CHALLENGE 3 - see docs/CHALLENGES.md.
 *
 * Return a NEW array (never mutate `auctions`) that:
 *   1. keeps only auctions whose title or description contains `filter.search`
 *      (case-insensitive, ignoring leading/trailing spaces; an empty search keeps everything)
 *   2. keeps only auctions in `filter.categorySlug` when it is not null
 *   3. is sorted by `filter.sort`:
 *        ENDING_SOON -> earliest endTime first
 *        NEWEST      -> latest startTime first
 *        PRICE_LOW   -> lowest currentPrice first
 *        PRICE_HIGH  -> highest currentPrice first
 *
 * Then change `describe.skip` to `describe` in filterAuctions.test.ts and run `npm test`.
 */
export function filterAuctions(auctions: readonly Auction[], filter: AuctionFilter): Auction[] {
  void filter
  return [...auctions]
}
