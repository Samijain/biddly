import Container from '@mui/material/Container'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import { useState } from 'react'
import { useNavigate } from 'react-router'

import { newAuctionFormValues } from '../features/auctions/auctionForm'
import { AuctionForm } from '../features/auctions/components/AuctionForm'
import { useCreateAuction } from '../features/auctions/hooks'

export function CreateAuctionPage() {
  const navigate = useNavigate()
  const createAuction = useCreateAuction()
  const [defaultValues] = useState(newAuctionFormValues)

  return (
    <Container maxWidth="md" sx={{ py: { xs: 4, md: 6 } }}>
      <Stack spacing={1} sx={{ mb: 4 }}>
        <Typography variant="h3" component="h1">
          Sell an item
        </Typography>
        <Typography color="text.secondary">
          Your auction is saved as a draft. You can review it before publishing.
        </Typography>
      </Stack>

      <AuctionForm
        defaultValues={defaultValues}
        submitLabel="Save draft"
        submitting={createAuction.isPending}
        error={createAuction.error}
        onSubmit={(input) => createAuction.mutate(input, { onSuccess: (auction) => navigate(`/auctions/${auction.id}`) })}
      />
    </Container>
  )
}
