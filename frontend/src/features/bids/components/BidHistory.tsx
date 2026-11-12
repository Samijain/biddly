import Avatar from '@mui/material/Avatar'
import Box from '@mui/material/Box'
import List from '@mui/material/List'
import ListItem from '@mui/material/ListItem'
import ListItemAvatar from '@mui/material/ListItemAvatar'
import ListItemText from '@mui/material/ListItemText'
import Skeleton from '@mui/material/Skeleton'
import Typography from '@mui/material/Typography'

import { formatDateTime, formatPrice } from '../../../lib/format'
import { useBids } from '../hooks'

export function BidHistory({ auctionId }: { auctionId: number }) {
  const { data: bids, isLoading, isError } = useBids(auctionId)

  return (
    <Box>
      <Typography variant="h6" component="h2" gutterBottom>
        Bid history
      </Typography>
      {isLoading && <Skeleton variant="rounded" height={72} />}
      {isError && <Typography color="text.secondary">Couldn't load the bids.</Typography>}
      {bids?.length === 0 && <Typography color="text.secondary">No bids yet. Be the first!</Typography>}
      {bids && bids.length > 0 && (
        <List disablePadding>
          {bids.map((bid, index) => (
            <ListItem key={bid.id} disableGutters divider={index < bids.length - 1}>
              <ListItemAvatar>
                <Avatar sx={{ width: 32, height: 32, fontSize: 14, bgcolor: index === 0 ? 'primary.main' : 'action.disabled' }}>
                  {bid.bidder.displayName.charAt(0)}
                </Avatar>
              </ListItemAvatar>
              <ListItemText primary={bid.bidder.displayName} secondary={formatDateTime(bid.placedAt)} />
              <Typography sx={{ fontWeight: index === 0 ? 700 : 500, fontVariantNumeric: 'tabular-nums' }}>
                {formatPrice(bid.amount)}
              </Typography>
            </ListItem>
          ))}
        </List>
      )}
    </Box>
  )
}
