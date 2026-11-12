import ArrowBackRounded from '@mui/icons-material/ArrowBackRounded'
import SearchOffRounded from '@mui/icons-material/SearchOffRounded'
import Avatar from '@mui/material/Avatar'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Card from '@mui/material/Card'
import CardContent from '@mui/material/CardContent'
import Chip from '@mui/material/Chip'
import Container from '@mui/material/Container'
import Divider from '@mui/material/Divider'
import Grid from '@mui/material/Grid'
import Skeleton from '@mui/material/Skeleton'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import { Link as RouterLink, useParams } from 'react-router'

import { EmptyState } from '../components/EmptyState'
import { AuctionActions } from '../features/auctions/components/AuctionActions'
import { AuctionImage } from '../features/auctions/components/AuctionImage'
import { Countdown } from '../features/auctions/components/Countdown'
import { useAuction } from '../features/auctions/hooks'
import type { AuctionStatus } from '../features/auctions/types'
import { BidHistory } from '../features/bids/components/BidHistory'
import { BidPanel } from '../features/bids/components/BidPanel'
import { formatDateTime, formatPrice } from '../lib/format'

const statusColor: Record<AuctionStatus, 'default' | 'info' | 'success' | 'error'> = {
  DRAFT: 'default',
  SCHEDULED: 'info',
  ACTIVE: 'success',
  COMPLETED: 'default',
  CANCELLED: 'error',
}

export function AuctionDetailsPage() {
  const { auctionId } = useParams()
  const id = Number(auctionId)
  const { data: auction, isLoading, isError, error } = useAuction(id)

  if (isError) {
    return (
      <Container maxWidth="lg">
        <EmptyState
          icon={<SearchOffRounded />}
          title="Auction not found"
          description={error.message}
          action={<Button component={RouterLink} to="/auctions">Back to auctions</Button>}
        />
      </Container>
    )
  }

  return (
    <Container maxWidth="lg" sx={{ py: { xs: 3, md: 5 } }}>
      <Button component={RouterLink} to="/auctions" startIcon={<ArrowBackRounded />} sx={{ mb: 3 }}>
        All auctions
      </Button>

      <Grid container spacing={{ xs: 3, md: 5 }}>
        <Grid size={{ xs: 12, md: 7 }}>
          <Card sx={{ overflow: 'hidden' }}>
            {isLoading || !auction ? (
              <Skeleton variant="rectangular" sx={{ aspectRatio: '4 / 3', height: 'auto' }} />
            ) : (
              <AuctionImage src={auction.coverImageUrl} alt={auction.title} />
            )}
          </Card>
          {auction?.description && (
            <Box sx={{ mt: 4 }}>
              <Typography variant="h6" component="h2" gutterBottom>
                About this item
              </Typography>
              <Typography color="text.secondary" sx={{ whiteSpace: 'pre-line' }}>
                {auction.description}
              </Typography>
            </Box>
          )}
          {auction && auction.status !== 'DRAFT' && (
            <Box sx={{ mt: 4 }}>
              <BidHistory auctionId={auction.id} />
            </Box>
          )}
        </Grid>

        <Grid size={{ xs: 12, md: 5 }}>
          {isLoading || !auction ? (
            <Stack spacing={2}>
              <Skeleton height={48} width="80%" />
              <Skeleton variant="rounded" height={260} />
            </Stack>
          ) : (
            <Stack spacing={3} sx={{ position: { md: 'sticky' }, top: { md: 96 } }}>
              <Stack spacing={1.5}>
                <Stack direction="row" spacing={1}>
                  {auction.category && <Chip size="small" label={auction.category.name} />}
                  <Chip size="small" color={statusColor[auction.status]} variant="outlined" label={auction.status} />
                </Stack>
                <Typography variant="h4" component="h1">
                  {auction.title}
                </Typography>
                <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                  <Avatar sx={{ width: 28, height: 28, fontSize: 14, bgcolor: 'primary.main' }}>
                    {auction.seller.displayName.charAt(0)}
                  </Avatar>
                  <Typography variant="body2" color="text.secondary">
                    Sold by {auction.seller.displayName}
                  </Typography>
                </Stack>
              </Stack>

              <AuctionActions auction={auction} />

              <Card>
                <CardContent sx={{ p: 3 }}>
                  <Stack spacing={2.5}>
                    <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'flex-start' }}>
                      <Box>
                        <Typography variant="body2" color="text.secondary">
                          Current bid
                        </Typography>
                        <Typography variant="h3" component="p" sx={{ fontVariantNumeric: 'tabular-nums' }}>
                          {formatPrice(auction.currentPrice)}
                        </Typography>
                      </Box>
                      <Countdown endTime={auction.endTime} size="medium" />
                    </Stack>

                    <Divider />

                    <BidPanel auction={auction} />
                  </Stack>
                </CardContent>
              </Card>

              <Stack spacing={1}>
                <Detail label="Starting price" value={formatPrice(auction.startingPrice)} />
                <Detail label="Minimum increment" value={formatPrice(auction.minimumIncrement)} />
                <Detail label="Started" value={formatDateTime(auction.startTime)} />
                <Detail label="Closes" value={formatDateTime(auction.endTime)} />
              </Stack>
            </Stack>
          )}
        </Grid>
      </Grid>
    </Container>
  )
}

function Detail({ label, value }: { label: string; value: string }) {
  return (
    <Stack direction="row" sx={{ justifyContent: 'space-between' }}>
      <Typography variant="body2" color="text.secondary">
        {label}
      </Typography>
      <Typography variant="body2" sx={{ fontWeight: 600 }}>
        {value}
      </Typography>
    </Stack>
  )
}
