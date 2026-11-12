/** A union of string literals instead of a TS `enum`: it is erased at compile time and matches the JSON exactly. */
export type AuctionStatus = 'DRAFT' | 'SCHEDULED' | 'ACTIVE' | 'COMPLETED' | 'CANCELLED'

export interface CategorySummary {
  id: number
  name: string
  slug: string
}

export interface SellerSummary {
  id: number
  displayName: string
}

/** Mirrors com.biddly.auction.dto.AuctionResponse. Keep the two in sync. */
export interface Auction {
  id: number
  title: string
  description: string | null
  coverImageUrl: string | null
  category: CategorySummary | null
  seller: SellerSummary
  startingPrice: number
  currentPrice: number
  minimumIncrement: number
  /** Null until the first bid is accepted. */
  leadingBidder: SellerSummary | null
  startTime: string
  endTime: string
  status: AuctionStatus
  serverTime: string
}

/** The editable fields of an auction, as sent to POST (create) and PUT (update). Times are ISO-8601 UTC. */
export interface AuctionInput {
  categoryId: number | null
  title: string
  description: string | null
  coverImageUrl: string | null
  startingPrice: number
  minimumIncrement: number
  startTime: string
  endTime: string
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export type AuctionSort = 'ENDING_SOON' | 'NEWEST' | 'PRICE_LOW' | 'PRICE_HIGH'

export interface AuctionFilter {
  search: string
  categorySlug: string | null
  sort: AuctionSort
}
