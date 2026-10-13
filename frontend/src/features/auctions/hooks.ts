import { useQuery } from '@tanstack/react-query'

import { fetchAuction, fetchAuctions, fetchCategories } from './api'
import type { AuctionStatus } from './types'

/**
 * Query keys identify cached server data. Anything the request depends on (status, id)
 * must be part of the key, otherwise two different requests would share one cache entry.
 */
export const auctionKeys = {
  all: ['auctions'] as const,
  list: (status: AuctionStatus) => [...auctionKeys.all, 'list', status] as const,
  detail: (id: number) => [...auctionKeys.all, 'detail', id] as const,
}

export function useAuctions(status: AuctionStatus = 'ACTIVE') {
  return useQuery({
    queryKey: auctionKeys.list(status),
    queryFn: () => fetchAuctions(status),
    // Level 1 "real-time": poll every 10 s. Replaced by WebSocket push in Milestone 7.
    refetchInterval: 10_000,
  })
}

export function useAuction(id: number) {
  return useQuery({
    queryKey: auctionKeys.detail(id),
    queryFn: () => fetchAuction(id),
    refetchInterval: 5_000,
  })
}

export function useCategories() {
  return useQuery({ queryKey: ['categories'], queryFn: fetchCategories, staleTime: Infinity })
}
