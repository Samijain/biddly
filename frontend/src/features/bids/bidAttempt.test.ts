import { describe, expect, it } from 'vitest'

import { nextAttempt } from './bidAttempt'

describe('nextAttempt', () => {
  let counter = 0
  const newKey = () => `key-${++counter}`

  it('starts a new attempt with a fresh key', () => {
    expect(nextAttempt(null, 2, 1000, newKey)).toEqual({ bidderId: 2, amount: 1000, idempotencyKey: 'key-1' })
  })

  it('reuses the key when the same bid is sent again', () => {
    const first = nextAttempt(null, 2, 1000, newKey)
    expect(nextAttempt(first, 2, 1000, newKey).idempotencyKey).toBe(first.idempotencyKey)
  })

  it('uses a new key when the amount or the bidder changes', () => {
    const first = nextAttempt(null, 2, 1000, newKey)
    expect(nextAttempt(first, 2, 1050, newKey).idempotencyKey).not.toBe(first.idempotencyKey)
    expect(nextAttempt(first, 3, 1000, newKey).idempotencyKey).not.toBe(first.idempotencyKey)
  })
})
