import { zodResolver } from '@hookform/resolvers/zod'
import Alert from '@mui/material/Alert'
import Button from '@mui/material/Button'
import Card from '@mui/material/Card'
import CardContent from '@mui/material/CardContent'
import Container from '@mui/material/Container'
import Grid from '@mui/material/Grid'
import InputAdornment from '@mui/material/InputAdornment'
import MenuItem from '@mui/material/MenuItem'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { useNavigate } from 'react-router'
import { z } from 'zod'

import { useCategories } from '../features/auctions/hooks'
import type { Auction } from '../features/auctions/types'
import { ApiError, http } from '../lib/http'

/** Until sign-in exists (Milestone 5) every auction is created by the seeded seller "Asha". */
const DEMO_SELLER_ID = 1

/** Client-side validation gives instant feedback. The server still re-validates everything. */
const schema = z
  .object({
    title: z.string().trim().min(3, 'At least 3 characters').max(120),
    description: z.string().max(5000).optional(),
    categoryId: z.string(),
    coverImageUrl: z.union([z.literal(''), z.url('Enter a valid URL')]),
    startingPrice: z.number({ error: 'Enter a price' }).min(1, 'Must be at least ₹1'),
    minimumIncrement: z.number({ error: 'Enter an increment' }).min(1, 'Must be at least ₹1'),
    startTime: z.string().min(1, 'Required'),
    endTime: z.string().min(1, 'Required'),
  })
  .refine((v) => new Date(v.endTime) > new Date(v.startTime), {
    path: ['endTime'],
    message: 'Must be after the start time',
  })

type FormValues = z.infer<typeof schema>

function toLocalInput(date: Date): string {
  const offsetMs = date.getTimezoneOffset() * 60_000
  return new Date(date.getTime() - offsetMs).toISOString().slice(0, 16)
}

function buildDefaults(): Partial<FormValues> {
  const now = Date.now()
  return {
    title: '',
    description: '',
    categoryId: '',
    coverImageUrl: '',
    startTime: toLocalInput(new Date(now + 10 * 60_000)),
    endTime: toLocalInput(new Date(now + 3 * 86_400_000)),
  }
}

export function CreateAuctionPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { data: categories = [] } = useCategories()
  const [defaultValues] = useState(buildDefaults)

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<FormValues>({ resolver: zodResolver(schema), defaultValues })

  const createAuction = useMutation({
    mutationFn: (values: FormValues) =>
      http<Auction>('/api/v1/auctions', {
        method: 'POST',
        body: JSON.stringify({
          sellerId: DEMO_SELLER_ID,
          categoryId: values.categoryId ? Number(values.categoryId) : null,
          title: values.title,
          description: values.description || null,
          coverImageUrl: values.coverImageUrl || null,
          startingPrice: values.startingPrice,
          minimumIncrement: values.minimumIncrement,
          // datetime-local has no timezone; new Date() reads it as local time, toISOString() sends UTC.
          startTime: new Date(values.startTime).toISOString(),
          endTime: new Date(values.endTime).toISOString(),
        }),
      }),
    onSuccess: async (auction) => {
      await queryClient.invalidateQueries({ queryKey: ['auctions'] })
      navigate(`/auctions/${auction.id}`)
    },
    onError: (error) => {
      if (error instanceof ApiError) {
        for (const [field, message] of Object.entries(error.fieldErrors)) {
          setError(field as keyof FormValues, { message })
        }
      }
    },
  })

  const apiError = createAuction.error instanceof ApiError ? createAuction.error : null

  return (
    <Container maxWidth="md" sx={{ py: { xs: 4, md: 6 } }}>
      <Stack spacing={1} sx={{ mb: 4 }}>
        <Typography variant="h3" component="h1">
          Sell an item
        </Typography>
        <Typography color="text.secondary">
          Your auction is saved as a draft. You can review it before publishing.
        </Typography>
      </Stack>

      <Card>
        <CardContent sx={{ p: { xs: 2.5, md: 4 } }}>
          <Stack component="form" spacing={3} noValidate onSubmit={handleSubmit((v) => createAuction.mutate(v))}>
            {apiError && (
              <Alert severity={apiError.code === 'NOT_IMPLEMENTED' ? 'info' : 'error'}>
                {apiError.code === 'NOT_IMPLEMENTED'
                  ? 'The server cannot create auctions yet - this is your backend Challenge 2!'
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
                <TextField select fullWidth label="Category" defaultValue="" {...register('categoryId')}>
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
              <Button type="submit" variant="contained" size="large" loading={createAuction.isPending}>
                Save draft
              </Button>
            </Stack>
          </Stack>
        </CardContent>
      </Card>
    </Container>
  )
}
