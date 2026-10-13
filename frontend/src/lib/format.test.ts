import { formatPrice, formatTimeLeft, timeLeft } from './format'

describe('formatPrice', () => {
  it('uses Indian digit grouping', () => {
    expect(formatPrice(1250000)).toBe('₹12,50,000')
  })
})

describe('timeLeft', () => {
  const now = Date.parse('2026-10-13T10:00:00Z')

  it('never goes negative after the auction ends', () => {
    expect(timeLeft('2026-10-13T09:00:00Z', now).totalMs).toBe(0)
    expect(formatTimeLeft(timeLeft('2026-10-13T09:00:00Z', now))).toBe('Ended')
  })

  it('shows hh:mm:ss under a day and days otherwise', () => {
    expect(formatTimeLeft(timeLeft('2026-10-13T11:02:03Z', now))).toBe('01:02:03')
    expect(formatTimeLeft(timeLeft('2026-10-15T13:30:00Z', now))).toBe('2d 03h 30m')
  })
})
