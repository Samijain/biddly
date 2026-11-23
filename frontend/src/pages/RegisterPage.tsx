import { zodResolver } from '@hookform/resolvers/zod'
import Alert from '@mui/material/Alert'
import Button from '@mui/material/Button'
import Card from '@mui/material/Card'
import CardContent from '@mui/material/CardContent'
import Container from '@mui/material/Container'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { NavLink } from 'react-router'

import { useRegister } from '../features/auth/hooks'
import { emptyRegisterForm, registerFormSchema, toRegisterInput, type RegisterFormValues } from '../features/auth/registerForm'
import { ApiError } from '../lib/http'

/** Server error codes that belong next to one field rather than above the form. */
const fieldForCode: Partial<Record<string, keyof RegisterFormValues>> = {
  EMAIL_TAKEN: 'email',
  PASSWORD_TOO_LONG: 'password',
}

export function RegisterPage() {
  const registerAccount = useRegister()
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<RegisterFormValues>({ resolver: zodResolver(registerFormSchema), defaultValues: emptyRegisterForm })

  const error = registerAccount.error
  useEffect(() => {
    if (!(error instanceof ApiError)) return
    for (const [field, message] of Object.entries(error.fieldErrors)) {
      setError(field as keyof RegisterFormValues, { message })
    }
    const field = fieldForCode[error.code]
    if (field) setError(field, { message: error.message })
  }, [error, setError])

  const apiError = error instanceof ApiError && !fieldForCode[error.code] && Object.keys(error.fieldErrors).length === 0 ? error : null
  const account = registerAccount.data

  return (
    <Container maxWidth="sm" sx={{ py: { xs: 4, md: 6 } }}>
      <Stack spacing={1} sx={{ mb: 4 }}>
        <Typography variant="h3" component="h1">
          Create your account
        </Typography>
        <Typography color="text.secondary">Bid on live auctions, and sell your own once you're approved.</Typography>
      </Stack>

      <Card>
        <CardContent sx={{ p: { xs: 2.5, md: 4 } }}>
          {account ? (
            <Stack spacing={3}>
              <Alert severity="success">
                Welcome, {account.displayName}! Your account for {account.email} is ready. Signing in arrives in Challenge 11b.
              </Alert>
              <Button variant="contained" component={NavLink} to="/auctions" sx={{ alignSelf: 'flex-start' }}>
                Browse live auctions
              </Button>
            </Stack>
          ) : (
            <Stack
              component="form"
              spacing={3}
              noValidate
              onSubmit={handleSubmit((values) => registerAccount.mutate(toRegisterInput(values)))}
            >
              {apiError && (
                <Alert severity={apiError.code === 'NOT_IMPLEMENTED' ? 'info' : 'error'}>
                  {apiError.code === 'NOT_IMPLEMENTED'
                    ? "The server can't do this yet. It's one of your backend challenges!"
                    : apiError.message}
                </Alert>
              )}

              <TextField
                label="Name"
                autoComplete="name"
                {...register('displayName')}
                error={!!errors.displayName}
                helperText={errors.displayName?.message ?? 'Shown next to your bids'}
              />
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
                autoComplete="new-password"
                {...register('password')}
                error={!!errors.password}
                helperText={errors.password?.message ?? 'At least 8 characters'}
              />
              <TextField
                label="Confirm password"
                type="password"
                autoComplete="new-password"
                {...register('confirmPassword')}
                error={!!errors.confirmPassword}
                helperText={errors.confirmPassword?.message}
              />

              <Button type="submit" variant="contained" size="large" loading={registerAccount.isPending}>
                Create account
              </Button>
            </Stack>
          )}
        </CardContent>
      </Card>
    </Container>
  )
}
