import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { auctionKeys } from '../auctions/hooks'
import { fetchBids, placeBid } from './api'
import type { PlaceBidInput } from './types'

export const bidKeys = {
  list: (auctionId: number) => [...auctionKeys.detail(auctionId), 'bids'] as const,
}

export function useBids(auctionId: number) {
  return useQuery({
    queryKey: bidKeys.list(auctionId),
    queryFn: () => fetchBids(auctionId),
    // Same polling as the auction itself, so other people's bids show up. WebSockets replace this in Milestone 7.
    refetchInterval: 5_000,
  })
}

/**
 * A bid changes the auction's price and leader, and every list that shows it.
 * The bid list sits under the auction's detail key, so invalidating auctionKeys.all refreshes it too.
 */
export function usePlaceBid(auctionId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (input: PlaceBidInput) => placeBid(auctionId, input),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: auctionKeys.all }),
  })
}
