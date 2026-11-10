import CancelRounded from '@mui/icons-material/CancelRounded'
import RocketLaunchRounded from '@mui/icons-material/RocketLaunchRounded'
import Alert from '@mui/material/Alert'
import Button from '@mui/material/Button'
import Card from '@mui/material/Card'
import CardContent from '@mui/material/CardContent'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogContentText from '@mui/material/DialogContentText'
import DialogTitle from '@mui/material/DialogTitle'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import { useState } from 'react'

import { ApiError } from '../../../lib/http'
import { useCancelAuction, usePublishAuction } from '../hooks'
import type { Auction, AuctionStatus } from '../types'

const cancellable: readonly AuctionStatus[] = ['DRAFT', 'SCHEDULED', 'ACTIVE']

/**
 * Seller controls on the details page. The buttons only mirror the rules for a nicer UI;
 * the server enforces them (Challenge 5). Until sign-in exists (Milestone 5) everyone sees them.
 */
export function AuctionActions({ auction }: { auction: Auction }) {
  const publish = usePublishAuction()
  const cancel = useCancelAuction()
  const [confirmingCancel, setConfirmingCancel] = useState(false)

  const canPublish = auction.status === 'DRAFT'
  const canCancel = cancellable.includes(auction.status)
  if (!canPublish && !canCancel) return null

  const error = publish.error ?? cancel.error
  const busy = publish.isPending || cancel.isPending

  return (
    <Card variant="outlined">
      <CardContent sx={{ p: 3 }}>
        <Stack spacing={2}>
          <Typography variant="subtitle2" color="text.secondary">
            Seller actions
          </Typography>

          {error && <ActionError error={error} />}

          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5}>
            {canPublish && (
              <Button
                variant="contained"
                startIcon={<RocketLaunchRounded />}
                loading={publish.isPending}
                disabled={busy}
                onClick={() => {
                  cancel.reset()
                  publish.mutate(auction.id)
                }}
              >
                Publish
              </Button>
            )}
            {canCancel && (
              <Button
                color="error"
                variant="outlined"
                startIcon={<CancelRounded />}
                disabled={busy}
                onClick={() => setConfirmingCancel(true)}
              >
                Cancel auction
              </Button>
            )}
          </Stack>
        </Stack>
      </CardContent>

      <Dialog open={confirmingCancel} onClose={() => setConfirmingCancel(false)}>
        <DialogTitle>Cancel this auction?</DialogTitle>
        <DialogContent>
          <DialogContentText>
            “{auction.title}” will be taken down and can't be published again.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setConfirmingCancel(false)}>Keep it</Button>
          <Button
            color="error"
            variant="contained"
            loading={cancel.isPending}
            onClick={() => {
              publish.reset()
              cancel.mutate(auction.id, { onSettled: () => setConfirmingCancel(false) })
            }}
          >
            Cancel auction
          </Button>
        </DialogActions>
      </Dialog>
    </Card>
  )
}

function ActionError({ error }: { error: Error }) {
  if (error instanceof ApiError && error.code === 'NOT_IMPLEMENTED') {
    return <Alert severity="info">The server can't publish or cancel auctions yet. This is your backend Challenge 5!</Alert>
  }
  return <Alert severity="error">{error.message}</Alert>
}
