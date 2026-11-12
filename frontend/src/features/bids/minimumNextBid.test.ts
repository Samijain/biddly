import { describe, expect, it } from 'vitest'

import { minimumNextBid } from './minimumNextBid'

describe('minimumNextBid', () => {
  it('is the starting price while nobody has bid', () => {
    expect(minimumNextBid({ leadingBidder: null, startingPrice: 1000, currentPrice: 1000, minimumIncrement: 50 })).toBe(1000)
  })

  it('adds the increment to the current price once there is a leader', () => {
    const leader = { id: 2, displayName: 'Rohan Bidder' }
    expect(minimumNextBid({ leadingBidder: leader, startingPrice: 1000, currentPrice: 1200, minimumIncrement: 50 })).toBe(1250)
  })
})
