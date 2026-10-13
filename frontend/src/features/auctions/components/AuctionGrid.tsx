import Grid from '@mui/material/Grid'

import type { Auction } from '../types'
import { AuctionCard } from './AuctionCard'
import { AuctionCardSkeleton } from './AuctionCardSkeleton'

interface AuctionGridProps {
  auctions: Auction[] | undefined
  loading: boolean
  skeletonCount?: number
}

export function AuctionGrid({ auctions, loading, skeletonCount = 6 }: AuctionGridProps) {
  return (
    <Grid container spacing={3}>
      {loading
        ? Array.from({ length: skeletonCount }, (_, i) => (
            <Grid key={i} size={{ xs: 12, sm: 6, md: 4 }}>
              <AuctionCardSkeleton />
            </Grid>
          ))
        : auctions?.map((auction) => (
            <Grid key={auction.id} size={{ xs: 12, sm: 6, md: 4 }}>
              <AuctionCard auction={auction} />
            </Grid>
          ))}
    </Grid>
  )
}
