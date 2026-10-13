import Box from '@mui/material/Box'
import Typography from '@mui/material/Typography'
import { Link as RouterLink } from 'react-router'

export function BrandLogo() {
  return (
    <Box
      component={RouterLink}
      to="/"
      sx={{ display: 'flex', alignItems: 'center', gap: 1.25, textDecoration: 'none', color: 'text.primary' }}
    >
      <Box component="img" src="/favicon.svg" alt="" sx={{ width: 32, height: 32 }} />
      <Typography variant="h6" component="span" sx={{ fontWeight: 800, letterSpacing: '-0.03em' }}>
        Biddly
      </Typography>
    </Box>
  )
}
