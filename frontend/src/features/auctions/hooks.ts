import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { cancelAuction, fetchAuction, fetchAuctions, fetchCategories, publishAuction } from './api'
import type { Auction, AuctionStatus } from './types'

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

/**
 * Changing an auction's status moves it between lists (DRAFT -> ACTIVE), so every auction query is
 * invalidated. The detail entry is updated straight away from the response, so the page doesn't flicker.
 */
function useAuctionAction(action: (id: number) => Promise<Auction>) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: action,
    onSuccess: async (auction) => {
      queryClient.setQueryData(auctionKeys.detail(auction.id), auction)
      await queryClient.invalidateQueries({ queryKey: auctionKeys.all })
    },
  })
}

export function usePublishAuction() {
  return useAuctionAction(publishAuction)
}

export function useCancelAuction() {
  return useAuctionAction(cancelAuction)
}

export function useCategories() {
  return useQuery({ queryKey: ['categories'], queryFn: fetchCategories, staleTime: Infinity })
}
