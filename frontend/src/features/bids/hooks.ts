import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { auctionKeys } from '../auctions/hooks'
import { fetchBids, placeBid } from './api'
import type { BidAttempt } from './bidAttempt'

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
 * A rejected bid refreshes as well: "too low" or a conflict usually means someone else just bid.
 */
export function usePlaceBid(auctionId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ bidderId, amount, idempotencyKey }: BidAttempt) => placeBid(auctionId, { bidderId, amount }, idempotencyKey),
    onSettled: () => queryClient.invalidateQueries({ queryKey: auctionKeys.all }),
  })
}
