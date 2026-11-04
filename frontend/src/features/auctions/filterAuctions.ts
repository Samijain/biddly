import type { Auction, AuctionFilter } from './types'

/**
 * Returns a NEW array (never mutates `auctions`: it is TanStack Query's cached data) that:
 *   1. keeps only auctions whose title or description contains `filter.search`
 *      (case-insensitive, ignoring leading/trailing spaces; an empty search keeps everything)
 *   2. keeps only auctions in `filter.categorySlug` when it is not null
 *   3. is sorted by `filter.sort`:
 *        ENDING_SOON -> earliest endTime first
 *        NEWEST      -> latest startTime first
 *        PRICE_LOW   -> lowest currentPrice first
 *        PRICE_HIGH  -> highest currentPrice first
 */
export function filterAuctions(auctions: readonly Auction[], filter: AuctionFilter): Auction[] {
  const query = filter.search.trim().toLowerCase()

  return auctions
    .filter((auction) => {
      const matchesCategory = filter.categorySlug === null || auction.category?.slug === filter.categorySlug
      const matchesSearch =
        auction.title.toLowerCase().includes(query) || (auction.description?.toLowerCase().includes(query) ?? false)
      return matchesCategory && matchesSearch
    })
    .toSorted((a, b) => {
      switch (filter.sort) {
        case 'ENDING_SOON':
          return Date.parse(a.endTime) - Date.parse(b.endTime)
        case 'NEWEST':
          return Date.parse(b.startTime) - Date.parse(a.startTime)
        case 'PRICE_LOW':
          return a.currentPrice - b.currentPrice
        case 'PRICE_HIGH':
          return b.currentPrice - a.currentPrice
        default: {
          const unhandledSort: never = filter.sort
          return unhandledSort
        }
      }
    })
}
