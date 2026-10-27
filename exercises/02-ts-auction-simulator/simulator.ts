/**
 * Biddly console auction simulator.
 *
 * Rules (the same ones the real backend will enforce):
 *  - An auction is OPEN until closeAuction() is called, then CLOSED.
 *  - The seller may not bid on their own auction.
 *  - The first bid must be at least startingPrice.
 *  - Every later bid must be at least currentPrice + minimumIncrement.
 *  - Bids can only be placed on an OPEN auction.
 *  - Functions never mutate their inputs: they return new objects.
 *
 * Shapes:
 *  Auction = { id, title, sellerId, startingPrice, minimumIncrement, currentPrice, status, bids: Bid[] }
 *  Bid     = { id, bidderId, amount, placedAt }   // placedAt: a Date
 */

import type { Auction, Bid, BidRejectionCode, ClosedAuction, CreateAuctionInput, OpenAuction, Result } from "./types.ts"

export class BidRejectedError extends Error {
  /** @param {string} code e.g. 'BID_TOO_LOW' */
  readonly code: BidRejectionCode;
  constructor(code: BidRejectionCode, message: string) {
    super(message)
    this.name = 'BidRejectedError'
    this.code = code
  }
}

/**
 * TODO: return a function that yields 'auction-1', 'auction-2', ... on each call (or 'bid-1', ...).
 * Each generator must keep its OWN counter. This is a closure.
 * @param {string} prefix
 * @returns {() => string}
 */
export function createIdGenerator(prefix: string): () => string {
  let counter = 0
  return () => `${prefix}-${++counter}`
}

// TODO: once createIdGenerator works, uncomment these and use them in createAuction / placeBid.
const nextAuctionId = createIdGenerator('auction')
const nextBidId = createIdGenerator('bid')

/**
 * TODO: return a new OPEN auction with no bids and currentPrice === startingPrice.
 * Throw a plain Error if startingPrice or minimumIncrement is not a positive number.
 * @param {{ title: string, sellerId: string, startingPrice: number, minimumIncrement: number }} input
 */
export function createAuction({ title, sellerId, startingPrice, minimumIncrement }: CreateAuctionInput): OpenAuction {
  if (!Number.isFinite(startingPrice) || !Number.isFinite(minimumIncrement) || (startingPrice <= 0 || minimumIncrement <= 0)) {
    throw new Error('Starting price and minimum increment must be positive numbers')
  }
  return {
    id: nextAuctionId(),
    title,
    sellerId,
    startingPrice,
    minimumIncrement,
    currentPrice: startingPrice,
    status: 'OPEN',
    bids: [],
  }
}

/**
 * TODO: validate, then return a NEW auction with the bid appended and currentPrice updated.
 * Throw BidRejectedError with code:
 *   'AUCTION_CLOSED'           if the auction is not OPEN
 *   'SELF_BIDDING_NOT_ALLOWED' if bidderId === sellerId
 *   'BID_TOO_LOW'              if amount is below the minimum (include the minimum in the message)
 * @param {object} auction
 * @param {string} bidderId
 * @param {number} amount
 * @param {Date} [now]
 */
export function placeBid(auction: OpenAuction, bidderId: string, amount: number, now = new Date()): OpenAuction {
 if(auction.status !== 'OPEN') {
  throw new BidRejectedError('AUCTION_CLOSED', 'Auction is not open')
 }
 if(bidderId === auction.sellerId) {
  throw new BidRejectedError('SELF_BIDDING_NOT_ALLOWED', 'Seller cannot bid on their own auction')
 }
 const minimumAmount= auction.bids.length === 0 ? auction.startingPrice : auction.currentPrice + auction.minimumIncrement
 if(amount < minimumAmount) {
  throw new BidRejectedError('BID_TOO_LOW', `Amount must be at least ${minimumAmount}`)
 }

 return {
    ...auction,
    currentPrice: amount,
    bids: [...auction.bids, { id: nextBidId(), bidderId, amount, placedAt: now }],
  }
}

/**
 * TODO: return the bid with the highest amount, or null if there are none. Try Array.prototype.reduce.
 * @param {object} auction
 */
export function getHighestBid(auction: Auction): Bid | null {
  return auction.bids.reduce<Bid | null>(
    (highest, bid) => (highest === null || bid.amount > highest.amount ? bid : highest),
    null,
  )
}

/**
 * TODO: return a NEW auction with status 'CLOSED'. Closing an already-closed auction throws.
 * @param {object} auction
 */
export function closeAuction(auction: OpenAuction): ClosedAuction {
  if (auction.status !== 'OPEN') {
    throw new Error('auction is already closed')
  }

  return {
    ...auction,
    status: 'CLOSED',
    winningBid: getHighestBid(auction),
  }
}

/**
 * TODO: return the winning bidderId of a CLOSED auction, null if it had no bids.
 * Throw if the auction is still OPEN - there is no winner yet.
 * @param {object} auction
 */
export function getWinner(auction: ClosedAuction): string | null {
  if ((auction as Auction).status === 'OPEN') {
    throw new Error('an open auction has no winner yet')
  }
  return auction.winningBid?.bidderId ?? null
}

export function isOpen(auction: Auction): auction is OpenAuction {
  return auction.status === 'OPEN'
}

export function tryPlaceBid(
  auction: OpenAuction,
  bidderId: string,
  amount: number,
  now = new Date(),
): Result<OpenAuction, BidRejectedError> {
  try {
    return { ok: true, value: placeBid(auction, bidderId, amount, now) }
  } catch (error: unknown) {
    if (error instanceof BidRejectedError) {
      return { ok: false, error }
    }
    throw error
  }
}

function isPositiveNumber(value: unknown): value is number {
  return typeof value === 'number' && Number.isFinite(value) && value > 0
}

function isNonBlankString(value: unknown): value is string {
  return typeof value === 'string' && value.trim() !== ''
}

export function parseCreateAuctionInput(value: unknown): CreateAuctionInput {
  if (typeof value !== 'object' || value === null || Array.isArray(value)) {
    throw new Error('input must be an object')
  }
  const { title, sellerId, startingPrice, minimumIncrement } = value as Record<string, unknown>

  if (!isNonBlankString(title)) throw new Error('title must be a non-blank string')
  if (!isNonBlankString(sellerId)) throw new Error('sellerId must be a non-blank string')
  if (!isPositiveNumber(startingPrice)) throw new Error('startingPrice must be a positive number')
  if (!isPositiveNumber(minimumIncrement)) throw new Error('minimumIncrement must be a positive number')

  return { title, sellerId, startingPrice, minimumIncrement }
}
