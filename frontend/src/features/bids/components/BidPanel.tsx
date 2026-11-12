import GavelRounded from '@mui/icons-material/GavelRounded'
import Alert from '@mui/material/Alert'
import Button from '@mui/material/Button'
import InputAdornment from '@mui/material/InputAdornment'
import MenuItem from '@mui/material/MenuItem'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import { type FormEvent, useState } from 'react'

import { ApiError } from '../../../lib/http'
import { formatPrice } from '../../../lib/format'
import type { Auction } from '../../auctions/types'
import { DEMO_BIDDERS } from '../api'
import { usePlaceBid } from '../hooks'
import { minimumNextBid } from '../minimumNextBid'

export function BidPanel({ auction }: { auction: Auction }) {
  const placeBid = usePlaceBid(auction.id)
  const [bidderId, setBidderId] = useState<number>(DEMO_BIDDERS[0].id)
  const [amount, setAmount] = useState('')
  const [placed, setPlaced] = useState<number | null>(null)

  const minimum = minimumNextBid(auction)
  const live = auction.status === 'ACTIVE'

  const submit = (event: FormEvent) => {
    event.preventDefault()
    setPlaced(null)
    const value = Number(amount)
    placeBid.mutate(
      { bidderId, amount: value },
      {
        onSuccess: () => {
          setPlaced(value)
          setAmount('')
        },
      },
    )
  }

  return (
    <Stack component="form" spacing={2.5} onSubmit={submit} noValidate>
      <Typography variant="body2" color="text.secondary">
        {auction.leadingBidder ? `Leading: ${auction.leadingBidder.displayName}` : 'No bids yet'}
      </Typography>

      <TextField
        select
        label="Bid as"
        value={bidderId}
        onChange={(event) => setBidderId(Number(event.target.value))}
        disabled={!live}
        helperText="Demo only: sign-in arrives in Milestone 5."
      >
        {DEMO_BIDDERS.map((bidder) => (
          <MenuItem key={bidder.id} value={bidder.id}>
            {bidder.displayName}
          </MenuItem>
        ))}
      </TextField>

      <TextField
        label="Your bid"
        type="number"
        value={amount}
        onChange={(event) => setAmount(event.target.value)}
        disabled={!live}
        helperText={`Minimum next bid: ${formatPrice(minimum)}`}
        slotProps={{
          input: { startAdornment: <InputAdornment position="start">₹</InputAdornment> },
          htmlInput: { min: minimum, step: '0.01' },
        }}
      />
      <Button
        type="submit"
        size="large"
        variant="contained"
        startIcon={<GavelRounded />}
        loading={placeBid.isPending}
        disabled={!live || amount === ''}
      >
        Place bid
      </Button>

      {!live && (
        <Alert severity="info" variant="outlined">
          Bids can only be placed while the auction is live.
        </Alert>
      )}
      {placeBid.error && <BidError error={placeBid.error} />}
      {placed !== null && !placeBid.error && <Alert severity="success">Your bid of {formatPrice(placed)} is in.</Alert>}
    </Stack>
  )
}

function BidError({ error }: { error: Error }) {
  if (error instanceof ApiError && error.code === 'NOT_IMPLEMENTED') {
    return <Alert severity="info">The server can't take bids yet. It's one of your backend challenges!</Alert>
  }
  if (error instanceof ApiError && error.code === 'CONCURRENT_UPDATE') {
    return <Alert severity="warning">Someone else bid at the same moment. The price above is now up to date. Check the minimum and try again.</Alert>
  }
  return <Alert severity="error">{error.message}</Alert>
}
