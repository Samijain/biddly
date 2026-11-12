import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { cancelAuction, createAuction, fetchAuction, fetchAuctions, fetchCategories, publishAuction, updateAuction } from './api'
import type { Auction, AuctionInput, AuctionStatus } from './types'

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
 * Any change to an auction can move it between lists (DRAFT -> ACTIVE) or change what a list shows, so every
 * auction query is invalidated. The detail entry is updated straight away from the response, so the page doesn't flicker.
 */
function useAuctionMutation<TVariables>(mutationFn: (variables: TVariables) => Promise<Auction>) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn,
    onSuccess: async (auction) => {
      queryClient.setQueryData(auctionKeys.detail(auction.id), auction)
      await queryClient.invalidateQueries({ queryKey: auctionKeys.all })
    },
  })
}

export function useCreateAuction() {
  return useAuctionMutation(createAuction)
}

export function useUpdateAuction() {
  return useAuctionMutation(({ id, input }: { id: number; input: AuctionInput }) => updateAuction(id, input))
}

export function usePublishAuction() {
  return useAuctionMutation(publishAuction)
}

export function useCancelAuction() {
  return useAuctionMutation(cancelAuction)
}

export function useCategories() {
  return useQuery({ queryKey: ['categories'], queryFn: fetchCategories, staleTime: Infinity })
}
