import ArrowForwardRounded from '@mui/icons-material/ArrowForwardRounded'
import BoltRounded from '@mui/icons-material/BoltRounded'
import GavelRounded from '@mui/icons-material/GavelRounded'
import VerifiedUserRounded from '@mui/icons-material/VerifiedUserRounded'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Container from '@mui/material/Container'
import Grid from '@mui/material/Grid'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import { Link as RouterLink } from 'react-router'

import { AuctionGrid } from '../features/auctions/components/AuctionGrid'
import { useAuctions } from '../features/auctions/hooks'

const features = [
  { icon: <BoltRounded />, title: 'Live prices', text: 'Watch bids land in real time and never miss the final seconds.' },
  { icon: <VerifiedUserRounded />, title: 'Fair by design', text: 'Every bid is validated and ordered by the server. One auction, one winner.' },
  { icon: <GavelRounded />, title: 'Sell in minutes', text: 'Set a starting price, a minimum step and a closing time. Done.' },
]

export function HomePage() {
  const { data, isLoading } = useAuctions('ACTIVE')
  const endingSoon = data?.content.toSorted((a, b) => Date.parse(a.endTime) - Date.parse(b.endTime)).slice(0, 3)

  return (
    <>
      <Box
        sx={(theme) => ({
          position: 'relative',
          overflow: 'hidden',
          py: { xs: 8, md: 14 },
          background: `radial-gradient(60% 80% at 15% 10%, rgba(${theme.vars.palette.primary.mainChannel} / 0.22), transparent 60%),
                       radial-gradient(50% 70% at 90% 30%, rgba(${theme.vars.palette.secondary.mainChannel} / 0.18), transparent 60%)`,
        })}
      >
        <Container maxWidth="lg">
          <Stack spacing={3} sx={{ maxWidth: 720 }}>
            <Typography variant="overline" color="primary">
              Live auctions · India
            </Typography>
            <Typography variant="h2" component="h1" sx={{ fontSize: { xs: '2.5rem', md: '3.75rem' } }}>
              Every second counts.{' '}
              <Box
                component="span"
                sx={{
                  background: 'linear-gradient(90deg, #6D4AFF, #FF6B4A)',
                  backgroundClip: 'text',
                  WebkitTextFillColor: 'transparent',
                }}
              >
                Bid live on Biddly.
              </Box>
            </Typography>
            <Typography variant="h6" component="p" color="text.secondary" sx={{ fontWeight: 400 }}>
              Discover rare finds, compete in real time and win at a fair price, with every bid recorded and verified.
            </Typography>
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
              <Button size="large" variant="contained" endIcon={<ArrowForwardRounded />} component={RouterLink} to="/auctions">
                Explore live auctions
              </Button>
              <Button size="large" variant="outlined" component={RouterLink} to="/auctions/new">
                Start selling
              </Button>
            </Stack>
          </Stack>
        </Container>
      </Box>

      <Container maxWidth="lg" sx={{ py: 6 }}>
        <Grid container spacing={3}>
          {features.map((f) => (
            <Grid key={f.title} size={{ xs: 12, md: 4 }}>
              <Stack spacing={1}>
                <Box sx={{ color: 'primary.main', '& svg': { fontSize: 32 } }}>{f.icon}</Box>
                <Typography variant="h6" component="h3">{f.title}</Typography>
                <Typography color="text.secondary">{f.text}</Typography>
              </Stack>
            </Grid>
          ))}
        </Grid>
      </Container>

      <Container maxWidth="lg" sx={{ pb: 10 }}>
        <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'baseline', mb: 3 }}>
          <Typography variant="h4" component="h2">
            Ending soon
          </Typography>
          <Button component={RouterLink} to="/auctions" endIcon={<ArrowForwardRounded />}>
            View all
          </Button>
        </Stack>
        <AuctionGrid auctions={endingSoon} loading={isLoading} skeletonCount={3} />
      </Container>
    </>
  )
}
