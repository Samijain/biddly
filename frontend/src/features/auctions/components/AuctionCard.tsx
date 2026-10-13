import Box from '@mui/material/Box'
import Card from '@mui/material/Card'
import CardActionArea from '@mui/material/CardActionArea'
import CardContent from '@mui/material/CardContent'
import Chip from '@mui/material/Chip'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import { Link as RouterLink } from 'react-router'

import { formatPrice } from '../../../lib/format'
import type { Auction } from '../types'
import { AuctionImage } from './AuctionImage'
import { Countdown } from './Countdown'

export function AuctionCard({ auction }: { auction: Auction }) {
  return (
    <Card
      sx={(theme) => ({
        height: '100%',
        '&:hover': {
          transform: 'translateY(-4px)',
          borderColor: theme.vars.palette.primary.main,
          boxShadow: `0 18px 40px -18px rgba(${theme.vars.palette.primary.mainChannel} / 0.45)`,
        },
      })}
    >
      <CardActionArea component={RouterLink} to={`/auctions/${auction.id}`} sx={{ height: '100%' }}>
        <Box sx={{ position: 'relative' }}>
          <AuctionImage src={auction.coverImageUrl} alt={auction.title} />
          <Box sx={{ position: 'absolute', top: 12, left: 12, right: 12, display: 'flex', justifyContent: 'space-between' }}>
            {auction.category ? (
              <Chip size="small" label={auction.category.name} sx={{ bgcolor: 'background.paper' }} />
            ) : (
              <span />
            )}
            <Box sx={{ bgcolor: 'background.paper', borderRadius: 999 }}>
              <Countdown endTime={auction.endTime} />
            </Box>
          </Box>
        </Box>
        <CardContent>
          <Typography variant="subtitle1" component="h3" sx={{ fontWeight: 650, mb: 1.5 }} noWrap title={auction.title}>
            {auction.title}
          </Typography>
          <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'flex-end' }}>
            <Box>
              <Typography variant="caption" color="text.secondary">
                Current bid
              </Typography>
              <Typography variant="h6" component="p" sx={{ lineHeight: 1.1, fontVariantNumeric: 'tabular-nums' }}>
                {formatPrice(auction.currentPrice)}
              </Typography>
            </Box>
            <Typography variant="caption" color="text.secondary">
              +{formatPrice(auction.minimumIncrement)} min
            </Typography>
          </Stack>
        </CardContent>
      </CardActionArea>
    </Card>
  )
}
