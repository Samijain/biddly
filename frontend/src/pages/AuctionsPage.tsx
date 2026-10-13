import CloudOffRounded from '@mui/icons-material/CloudOffRounded'
import SearchOffRounded from '@mui/icons-material/SearchOffRounded'
import Button from '@mui/material/Button'
import Container from '@mui/material/Container'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import { useDeferredValue, useMemo, useState } from 'react'

import { EmptyState } from '../components/EmptyState'
import { AuctionFilters } from '../features/auctions/components/AuctionFilters'
import { AuctionGrid } from '../features/auctions/components/AuctionGrid'
import { filterAuctions } from '../features/auctions/filterAuctions'
import { useAuctions } from '../features/auctions/hooks'
import type { AuctionFilter } from '../features/auctions/types'

const initialFilter: AuctionFilter = { search: '', categorySlug: null, sort: 'ENDING_SOON' }

export function AuctionsPage() {
  const { data, isLoading, isError, error, refetch } = useAuctions('ACTIVE')
  const [filter, setFilter] = useState<AuctionFilter>(initialFilter)
  // Typing stays responsive: React filters with the "deferred" value when it has time.
  const deferredFilter = useDeferredValue(filter)

  const visible = useMemo(
    () => (data ? filterAuctions(data.content, deferredFilter) : undefined),
    [data, deferredFilter],
  )

  return (
    <Container maxWidth="lg" sx={{ py: { xs: 4, md: 6 } }}>
      <Stack spacing={1} sx={{ mb: 4 }}>
        <Typography variant="h3" component="h1">
          Live auctions
        </Typography>
        <Typography color="text.secondary">
          {data ? `${data.totalElements} auctions open for bidding right now` : 'Finding the best deals for you...'}
        </Typography>
      </Stack>

      <Stack spacing={4}>
        <AuctionFilters value={filter} onChange={setFilter} />

        {isError ? (
          <EmptyState
            icon={<CloudOffRounded />}
            title="We couldn't load auctions"
            description={error.message}
            action={<Button variant="contained" onClick={() => refetch()}>Try again</Button>}
          />
        ) : !isLoading && visible?.length === 0 ? (
          <EmptyState
            icon={<SearchOffRounded />}
            title="No auctions match your filters"
            description="Try a different search term or category."
            action={<Button onClick={() => setFilter(initialFilter)}>Clear filters</Button>}
          />
        ) : (
          <AuctionGrid auctions={visible} loading={isLoading} />
        )}
      </Stack>
    </Container>
  )
}
