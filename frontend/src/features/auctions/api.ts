import { http } from '../../lib/http'
import { mockAuctions, mockCategories } from './mockAuctions'
import type { Auction, AuctionInput, AuctionStatus, CategorySummary, PageResponse } from './types'

const useMocks = import.meta.env.VITE_USE_MOCKS === 'true'

/** Until sign-in exists (Milestone 5) every auction is created by the seeded seller "Asha". */
const DEMO_SELLER_ID = 1
const delay = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms))

export async function fetchAuctions(status: AuctionStatus = 'ACTIVE'): Promise<PageResponse<Auction>> {
  if (useMocks) {
    await delay(400)
    const content = mockAuctions.filter((a) => a.status === status)
    return { content, page: 0, size: content.length, totalElements: content.length, totalPages: 1 }
  }
  return http<PageResponse<Auction>>(`/api/v1/auctions?status=${status}&size=50`)
}

export async function fetchAuction(id: number): Promise<Auction> {
  if (useMocks) {
    await delay(300)
    const found = mockAuctions.find((a) => a.id === id)
    if (!found) throw new Error(`Auction ${id} was not found.`)
    return found
  }
  return http<Auction>(`/api/v1/auctions/${id}`)
}

export async function createAuction(input: AuctionInput): Promise<Auction> {
  if (useMocks) throw new Error('Creating auctions needs the real backend. Run `npm run dev` instead of `npm run dev:mock`.')
  return http<Auction>('/api/v1/auctions', { method: 'POST', body: JSON.stringify({ sellerId: DEMO_SELLER_ID, ...input }) })
}

export async function updateAuction(id: number, input: AuctionInput): Promise<Auction> {
  if (useMocks) throw new Error('Editing auctions needs the real backend. Run `npm run dev` instead of `npm run dev:mock`.')
  return http<Auction>(`/api/v1/auctions/${id}`, { method: 'PUT', body: JSON.stringify(input) })
}

export async function publishAuction(id: number): Promise<Auction> {
  if (useMocks) throw new Error('Publishing needs the real backend. Run `npm run dev` instead of `npm run dev:mock`.')
  return http<Auction>(`/api/v1/auctions/${id}/publish`, { method: 'POST' })
}

export async function cancelAuction(id: number): Promise<Auction> {
  if (useMocks) throw new Error('Cancelling needs the real backend. Run `npm run dev` instead of `npm run dev:mock`.')
  return http<Auction>(`/api/v1/auctions/${id}/cancel`, { method: 'POST' })
}

export async function fetchCategories(): Promise<CategorySummary[]> {
  if (useMocks) return mockCategories
  return http<CategorySummary[]>('/api/v1/categories')
}
