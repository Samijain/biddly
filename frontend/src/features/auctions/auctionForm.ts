import { z } from 'zod'

import type { Auction, AuctionInput } from './types'

/** R24, mirrored from AuctionService. "4 months" is a fixed 120 days. */
const MIN_LENGTH_MS = 10 * 60_000
const MAX_LENGTH_MS = 120 * 86_400_000

function lengthMs(v: { startTime: string; endTime: string }): number {
  return new Date(v.endTime).getTime() - new Date(v.startTime).getTime()
}

/** Client-side validation gives instant feedback. The server still re-validates everything. */
export const auctionFormSchema = z
  .object({
    title: z.string().trim().min(3, 'At least 3 characters').max(120),
    description: z.string().max(5000).optional(),
    categoryId: z.string(),
    coverImageUrl: z.union([z.literal(''), z.url('Enter a valid URL')]),
    startingPrice: z.number({ error: 'Enter a price' }).int('Whole rupees only').min(1, 'Must be at least ₹1'),
    minimumIncrement: z.number({ error: 'Enter an increment' }).int('Whole rupees only').min(1, 'Must be at least ₹1'),
    startTime: z.string().min(1, 'Required'),
    endTime: z.string().min(1, 'Required'),
  })
  .refine((v) => new Date(v.endTime) > new Date(v.startTime), {
    path: ['endTime'],
    message: 'Must be after the start time',
  })
  .refine((v) => lengthMs(v) <= 0 || lengthMs(v) >= MIN_LENGTH_MS, {
    path: ['endTime'],
    message: 'An auction must last at least 10 minutes',
  })
  .refine((v) => lengthMs(v) <= MAX_LENGTH_MS, {
    path: ['endTime'],
    message: 'An auction can last at most 4 months (120 days)',
  })

export type AuctionFormValues = z.infer<typeof auctionFormSchema>

/** `datetime-local` inputs want "YYYY-MM-DDTHH:mm" in the viewer's local time, without a zone. */
function toLocalInput(date: Date): string {
  const offsetMs = date.getTimezoneOffset() * 60_000
  return new Date(date.getTime() - offsetMs).toISOString().slice(0, 16)
}

export function newAuctionFormValues(): Partial<AuctionFormValues> {
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

export function auctionFormValuesFrom(auction: Auction): AuctionFormValues {
  return {
    title: auction.title,
    description: auction.description ?? '',
    categoryId: auction.category ? String(auction.category.id) : '',
    coverImageUrl: auction.coverImageUrl ?? '',
    startingPrice: auction.startingPrice,
    minimumIncrement: auction.minimumIncrement,
    startTime: toLocalInput(new Date(auction.startTime)),
    endTime: toLocalInput(new Date(auction.endTime)),
  }
}

export function toAuctionInput(values: AuctionFormValues): AuctionInput {
  return {
    categoryId: values.categoryId ? Number(values.categoryId) : null,
    title: values.title,
    description: values.description || null,
    coverImageUrl: values.coverImageUrl || null,
    startingPrice: values.startingPrice,
    minimumIncrement: values.minimumIncrement,
    // datetime-local has no timezone; new Date() reads it as local time, toISOString() sends UTC.
    startTime: new Date(values.startTime).toISOString(),
    endTime: new Date(values.endTime).toISOString(),
  }
}
