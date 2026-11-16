/**
 * One "Place bid" attempt and its Idempotency-Key (R23).
 * Pressing the button again for the same bidder and amount, for example after a network error, is the same attempt:
 * it reuses the key, so the server can answer with the bid it already placed instead of placing a second one.
 */
export interface BidAttempt {
  bidderId: number
  amount: number
  idempotencyKey: string
}

export function nextAttempt(
  previous: BidAttempt | null,
  bidderId: number,
  amount: number,
  newKey: () => string = () => crypto.randomUUID(),
): BidAttempt {
  if (previous && previous.bidderId === bidderId && previous.amount === amount) return previous
  return { bidderId, amount, idempotencyKey: newKey() }
}
