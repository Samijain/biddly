import { zodResolver } from '@hookform/resolvers/zod'
import Alert from '@mui/material/Alert'
import Button from '@mui/material/Button'
import Card from '@mui/material/Card'
import CardContent from '@mui/material/CardContent'
import Container from '@mui/material/Container'
import Link from '@mui/material/Link'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import { useForm } from 'react-hook-form'
import { NavLink, useLocation, useNavigate } from 'react-router'
import { z } from 'zod'

import { useSignIn } from '../features/auth/hooks'
import { ApiError } from '../lib/http'

/** Only "is something there?": sign-in never explains which part was wrong (R28). */
const signInSchema = z.object({
  email: z.string().trim().min(1, 'Enter your email'),
  password: z.string().min(1, 'Enter your password'),
})

type SignInValues = z.infer<typeof signInSchema>

export function SignInPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const signIn = useSignIn()
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<SignInValues>({ resolver: zodResolver(signInSchema), defaultValues: { email: '', password: '' } })

  const from = (location.state as { from?: string } | null)?.from ?? '/auctions'
  const error = signIn.error instanceof ApiError ? signIn.error : null

  return (
    <Container maxWidth="sm" sx={{ py: { xs: 4, md: 6 } }}>
      <Stack spacing={1} sx={{ mb: 4 }}>
        <Typography variant="h3" component="h1">
          Sign in
        </Typography>
        <Typography color="text.secondary">
          No account yet?{' '}
          <Link component={NavLink} to="/register">
            Create one
          </Link>
        </Typography>
      </Stack>

      <Card>
        <CardContent sx={{ p: { xs: 2.5, md: 4 } }}>
          <Stack
            component="form"
            spacing={3}
            noValidate
            onSubmit={handleSubmit((values) => signIn.mutate(values, { onSuccess: () => navigate(from, { replace: true }) }))}
          >
            {error && (
              <Alert severity={error.code === 'NOT_IMPLEMENTED' ? 'info' : 'error'}>
                {error.code === 'NOT_IMPLEMENTED' ? "The server can't do this yet. It's one of your backend challenges!" : error.message}
              </Alert>
            )}
            {import.meta.env.DEV && (
              <Alert severity="info" variant="outlined">
                Demo accounts: asha@biddly.dev, rohan@biddly.dev or meera@biddly.dev, password <strong>biddly-demo</strong>.
              </Alert>
            )}

            <TextField
              label="Email"
              type="email"
              autoComplete="email"
              {...register('email')}
              error={!!errors.email}
              helperText={errors.email?.message}
            />
            <TextField
              label="Password"
              type="password"
              autoComplete="current-password"
              {...register('password')}
              error={!!errors.password}
              helperText={errors.password?.message}
            />

            <Button type="submit" variant="contained" size="large" loading={signIn.isPending}>
              Sign in
            </Button>
          </Stack>
        </CardContent>
      </Card>
    </Container>
  )
}
