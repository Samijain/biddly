import ExploreOffRounded from '@mui/icons-material/ExploreOffRounded'
import Button from '@mui/material/Button'
import Container from '@mui/material/Container'
import { Link as RouterLink } from 'react-router'

import { EmptyState } from '../components/EmptyState'

export function NotFoundPage() {
  return (
    <Container maxWidth="sm">
      <EmptyState
        icon={<ExploreOffRounded />}
        title="This page doesn't exist"
        description="The link may be broken or the auction may have been removed."
        action={<Button variant="contained" component={RouterLink} to="/">Go home</Button>}
      />
    </Container>
  )
}
