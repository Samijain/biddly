import { describe, expect, it } from 'vitest'

import { auctionFormSchema } from './auctionForm'

const base = {
  title: 'Brass lamp',
  description: '',
  categoryId: '',
  coverImageUrl: '',
  startingPrice: 1000,
  minimumIncrement: 50,
  startTime: '2030-01-01T10:00',
  endTime: '2030-01-02T10:00',
}

function errorsFor(values: Partial<typeof base>) {
  const result = auctionFormSchema.safeParse({ ...base, ...values })
  return result.success ? [] : result.error.issues.map((i) => `${i.path.join('.')}: ${i.message}`)
}

describe('auctionFormSchema', () => {
  it('accepts a valid auction', () => {
    expect(errorsFor({})).toEqual([])
  })

  it('accepts only whole rupees (R25)', () => {
    expect(errorsFor({ startingPrice: 1000.5 })).toEqual(['startingPrice: Whole rupees only'])
    expect(errorsFor({ minimumIncrement: 10.25 })).toEqual(['minimumIncrement: Whole rupees only'])
  })

  it('allows 10 minutes to 120 days (R24)', () => {
    expect(errorsFor({ endTime: '2030-01-01T10:10' })).toEqual([])
    expect(errorsFor({ endTime: '2030-01-01T10:09' })).toEqual(['endTime: An auction must last at least 10 minutes'])
    // 5 Nov 2030 to 5 Mar 2031 is exactly 120 days, with no daylight-saving change in the US or EU.
    const winter = { startTime: '2030-11-05T10:00' }
    expect(errorsFor({ ...winter, endTime: '2031-03-05T10:00' })).toEqual([])
    expect(errorsFor({ ...winter, endTime: '2031-03-05T10:01' })).toEqual(['endTime: An auction can last at most 4 months (120 days)'])
  })

  it('reports an end before the start only once', () => {
    expect(errorsFor({ endTime: '2030-01-01T09:00' })).toEqual(['endTime: Must be after the start time'])
  })
})
