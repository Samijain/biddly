import { http } from '../../lib/http'
import type { Bid, PlaceBidInput } from './types'

const useMocks = import.meta.env.VITE_USE_MOCKS === 'true'

/**
 * Until sign-in exists (Milestone 5) the bidder is picked from the seeded users.
 * Asha is the seller of every demo auction, so bidding as her shows the self-bidding rule (R3).
 */
export const DEMO_BIDDERS = [
  { id: 2, displayName: 'Rohan Bidder' },
  { id: 3, displayName: 'Meera Bidder' },
  { id: 1, displayName: 'Asha Seller' },
] as const

export async function fetchBids(auctionId: number): Promise<Bid[]> {
  if (useMocks) return []
  return http<Bid[]>(`/api/v1/auctions/${auctionId}/bids`)
}

export async function placeBid(auctionId: number, input: PlaceBidInput): Promise<Bid> {
  if (useMocks) throw new Error('Bidding needs the real backend. Run `npm run dev` instead of `npm run dev:mock`.')
  return http<Bid>(`/api/v1/auctions/${auctionId}/bids`, { method: 'POST', body: JSON.stringify(input) })
}
