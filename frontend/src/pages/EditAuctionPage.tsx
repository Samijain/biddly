import ArrowBackRounded from '@mui/icons-material/ArrowBackRounded'
import SearchOffRounded from '@mui/icons-material/SearchOffRounded'
import Alert from '@mui/material/Alert'
import Button from '@mui/material/Button'
import Container from '@mui/material/Container'
import Skeleton from '@mui/material/Skeleton'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import { Link as RouterLink, useNavigate, useParams } from 'react-router'

import { EmptyState } from '../components/EmptyState'
import { auctionFormValuesFrom } from '../features/auctions/auctionForm'
import { AuctionForm } from '../features/auctions/components/AuctionForm'
import { useAuction, useUpdateAuction } from '../features/auctions/hooks'

export function EditAuctionPage() {
  const { auctionId } = useParams()
  const id = Number(auctionId)
  const navigate = useNavigate()
  const { data: auction, isLoading, isError, error } = useAuction(id)
  const updateAuction = useUpdateAuction()

  if (isError) {
    return (
      <Container maxWidth="md">
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
    <Container maxWidth="md" sx={{ py: { xs: 4, md: 6 } }}>
      <Button component={RouterLink} to={`/auctions/${id}`} startIcon={<ArrowBackRounded />} sx={{ mb: 3 }}>
        Back to auction
      </Button>

      <Stack spacing={1} sx={{ mb: 4 }}>
        <Typography variant="h3" component="h1">
          Edit draft
        </Typography>
        <Typography color="text.secondary">
          Bidders can't see a draft, so you can change anything until you publish it.
        </Typography>
      </Stack>

      {isLoading || !auction ? (
        <Skeleton variant="rounded" height={560} />
      ) : auction.status !== 'DRAFT' ? (
        <Alert severity="warning">
          Only drafts can be edited. This auction is {auction.status.toLowerCase()}, so its details are locked.
        </Alert>
      ) : (
        <AuctionForm
          key={auction.id}
          defaultValues={auctionFormValuesFrom(auction)}
          submitLabel="Save changes"
          submitting={updateAuction.isPending}
          error={updateAuction.error}
          onSubmit={(input) => updateAuction.mutate({ id, input }, { onSuccess: () => navigate(`/auctions/${id}`) })}
        />
      )}
    </Container>
  )
}
