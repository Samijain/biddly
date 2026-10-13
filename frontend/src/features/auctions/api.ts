import { http } from '../../lib/http'
import { mockAuctions, mockCategories } from './mockAuctions'
import type { Auction, AuctionStatus, CategorySummary, PageResponse } from './types'

const useMocks = import.meta.env.VITE_USE_MOCKS === 'true'
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

export async function fetchCategories(): Promise<CategorySummary[]> {
  if (useMocks) return mockCategories
  return http<CategorySummary[]>('/api/v1/categories')
}
