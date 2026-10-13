import AddRounded from '@mui/icons-material/AddRounded'
import AppBar from '@mui/material/AppBar'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Container from '@mui/material/Container'
import Stack from '@mui/material/Stack'
import Toolbar from '@mui/material/Toolbar'
import Typography from '@mui/material/Typography'
import { NavLink, Outlet } from 'react-router'

import { BrandLogo } from '../BrandLogo'
import { ColorModeToggle } from '../ColorModeToggle'

const currentYear = new Date().getFullYear()

const navItems = [
  { to: '/', label: 'Home', end: true },
  { to: '/auctions', label: 'Live auctions', end: false },
]

export function AppLayout() {
  return (
    <Box sx={{ minHeight: '100vh', display: 'flex', flexDirection: 'column', bgcolor: 'background.default' }}>
      <AppBar
        position="sticky"
        color="transparent"
        elevation={0}
        sx={(theme) => ({
          backdropFilter: 'saturate(180%) blur(14px)',
          backgroundColor: `rgba(${theme.vars.palette.background.defaultChannel} / 0.72)`,
          borderBottom: `1px solid ${theme.vars.palette.divider}`,
        })}
      >
        <Container maxWidth="lg">
          <Toolbar disableGutters sx={{ gap: 3 }}>
            <BrandLogo />
            <Stack direction="row" spacing={0.5} component="nav" sx={{ display: { xs: 'none', sm: 'flex' } }}>
              {navItems.map((item) => (
                <Button
                  key={item.to}
                  component={NavLink}
                  to={item.to}
                  end={item.end}
                  color="inherit"
                  sx={{
                    color: 'text.secondary',
                    '&.active': { color: 'text.primary', bgcolor: 'action.selected' },
                  }}
                >
                  {item.label}
                </Button>
              ))}
            </Stack>
            <Box sx={{ flexGrow: 1 }} />
            <ColorModeToggle />
            <Button variant="contained" startIcon={<AddRounded />} component={NavLink} to="/auctions/new">
              Sell an item
            </Button>
          </Toolbar>
        </Container>
      </AppBar>

      <Box component="main" sx={{ flexGrow: 1 }}>
        <Outlet />
      </Box>

      <Box component="footer" sx={{ py: 4, borderTop: 1, borderColor: 'divider' }}>
        <Container maxWidth="lg">
          <Typography variant="body2" color="text.secondary">
            © {currentYear} Biddly. Built with React, Spring Boot and PostgreSQL.
          </Typography>
        </Container>
      </Box>
    </Box>
  )
}
