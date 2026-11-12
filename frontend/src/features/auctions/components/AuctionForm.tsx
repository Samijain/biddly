import { zodResolver } from '@hookform/resolvers/zod'
import Alert from '@mui/material/Alert'
import Button from '@mui/material/Button'
import Card from '@mui/material/Card'
import CardContent from '@mui/material/CardContent'
import Grid from '@mui/material/Grid'
import InputAdornment from '@mui/material/InputAdornment'
import MenuItem from '@mui/material/MenuItem'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { useNavigate } from 'react-router'

import { ApiError } from '../../../lib/http'
import { auctionFormSchema, toAuctionInput, type AuctionFormValues } from '../auctionForm'
import { useCategories } from '../hooks'
import type { AuctionInput } from '../types'

interface AuctionFormProps {
  defaultValues: Partial<AuctionFormValues>
  submitLabel: string
  submitting: boolean
  /** The last server error. Field errors are shown next to their fields; anything else above the form. */
  error: Error | null
  onSubmit: (input: AuctionInput) => void
}

/** The auction fields shared by "Sell an item" and "Edit draft". */
export function AuctionForm({ defaultValues, submitLabel, submitting, error, onSubmit }: AuctionFormProps) {
  const navigate = useNavigate()
  const { data: categories = [] } = useCategories()

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<AuctionFormValues>({ resolver: zodResolver(auctionFormSchema), defaultValues })

  useEffect(() => {
    if (!(error instanceof ApiError)) return
    for (const [field, message] of Object.entries(error.fieldErrors)) {
      setError(field as keyof AuctionFormValues, { message })
    }
  }, [error, setError])

  const apiError = error instanceof ApiError ? error : null

  return (
    <Card>
      <CardContent sx={{ p: { xs: 2.5, md: 4 } }}>
        <Stack component="form" spacing={3} noValidate onSubmit={handleSubmit((v) => onSubmit(toAuctionInput(v)))}>
          {apiError && (
            <Alert severity={apiError.code === 'NOT_IMPLEMENTED' ? 'info' : 'error'}>
              {apiError.code === 'NOT_IMPLEMENTED'
                ? "The server can't do this yet. It's one of your backend challenges!"
                : apiError.message}
            </Alert>
          )}

          <TextField label="Title" {...register('title')} error={!!errors.title} helperText={errors.title?.message} />
          <TextField
            label="Description"
            multiline
            minRows={4}
            {...register('description')}
            error={!!errors.description}
            helperText={errors.description?.message}
          />

          <Grid container spacing={2}>
            <Grid size={{ xs: 12, sm: 6 }}>
              {/* MUI's select keeps its own state, so it needs its starting value directly. */}
              <TextField select fullWidth label="Category" defaultValue={defaultValues.categoryId ?? ''} {...register('categoryId')}>
                <MenuItem value="">No category</MenuItem>
                {categories.map((c) => (
                  <MenuItem key={c.id} value={String(c.id)}>
                    {c.name}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
            <Grid size={{ xs: 12, sm: 6 }}>
              <TextField
                fullWidth
                label="Cover image URL"
                placeholder="https://..."
                {...register('coverImageUrl')}
                error={!!errors.coverImageUrl}
                helperText={errors.coverImageUrl?.message}
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 6 }}>
              <TextField
                fullWidth
                type="number"
                label="Starting price"
                {...register('startingPrice', { valueAsNumber: true })}
                error={!!errors.startingPrice}
                helperText={errors.startingPrice?.message}
                slotProps={{ input: { startAdornment: <InputAdornment position="start">₹</InputAdornment> } }}
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 6 }}>
              <TextField
                fullWidth
                type="number"
                label="Minimum increment"
                {...register('minimumIncrement', { valueAsNumber: true })}
                error={!!errors.minimumIncrement}
                helperText={errors.minimumIncrement?.message ?? 'Each new bid must beat the current one by at least this much'}
                slotProps={{ input: { startAdornment: <InputAdornment position="start">₹</InputAdornment> } }}
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 6 }}>
              <TextField
                fullWidth
                type="datetime-local"
                label="Starts"
                {...register('startTime')}
                error={!!errors.startTime}
                helperText={errors.startTime?.message}
                slotProps={{ inputLabel: { shrink: true } }}
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 6 }}>
              <TextField
                fullWidth
                type="datetime-local"
                label="Ends"
                {...register('endTime')}
                error={!!errors.endTime}
                helperText={errors.endTime?.message}
                slotProps={{ inputLabel: { shrink: true } }}
              />
            </Grid>
          </Grid>

          <Stack direction="row" spacing={2} sx={{ justifyContent: 'flex-end' }}>
            <Button onClick={() => navigate(-1)}>Cancel</Button>
            <Button type="submit" variant="contained" size="large" loading={submitting}>
              {submitLabel}
            </Button>
          </Stack>
        </Stack>
      </CardContent>
    </Card>
  )
}
