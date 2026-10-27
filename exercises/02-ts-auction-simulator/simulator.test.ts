import assert from 'node:assert/strict'
import { describe, it } from 'node:test'

import {
  BidRejectedError,
  closeAuction,
  createAuction,
  createIdGenerator,
  getHighestBid,
  getWinner,
  isOpen,
  parseCreateAuctionInput,
  placeBid,
  tryPlaceBid,
} from './simulator.ts'
import type { Auction, ClosedAuction, OpenAuction } from './types.ts'

const newAuction = (): OpenAuction =>
  createAuction({ title: 'Leica M3', sellerId: 'asha', startingPrice: 1000, minimumIncrement: 100 })

/**
 * Types disappear when the code runs. `untrusted` pretends a value has a type it may not really have,
 * the way JSON from a client might. Runtime checks must still catch it.
 */
const untrusted = <T>(value: unknown): T => value as T

const isBidError = (code: string) => (e: unknown) => e instanceof BidRejectedError && e.code === code

describe('createIdGenerator', () => {
  it('counts up per generator', () => {
    const a = createIdGenerator('x')
    const b = createIdGenerator('y')
    assert.equal(a(), 'x-1')
    assert.equal(a(), 'x-2')
    assert.equal(b(), 'y-1')
  })
})

describe('createAuction', () => {
  it('creates an open auction priced at the starting price', () => {
    const auction = newAuction()
    assert.equal(auction.status, 'OPEN')
    assert.equal(auction.currentPrice, 1000)
    assert.deepEqual(auction.bids, [])
    assert.match(auction.id, /^auction-\d+$/)
  })

  it('rejects non-positive and non-finite prices', () => {
    for (const bad of [0, -1, Number.NaN, Number.POSITIVE_INFINITY]) {
      assert.throws(() => createAuction({ title: 'x', sellerId: 's', startingPrice: bad, minimumIncrement: 10 }), `startingPrice ${bad}`)
      assert.throws(() => createAuction({ title: 'x', sellerId: 's', startingPrice: 10, minimumIncrement: bad }), `minimumIncrement ${bad}`)
    }
  })
})

describe('placeBid', () => {
  it('accepts a first bid equal to the starting price', () => {
    const after = placeBid(newAuction(), 'rohan', 1000)
    assert.equal(after.currentPrice, 1000)
    assert.equal(after.bids.length, 1)
  })

  it('requires later bids to beat current price by the increment', () => {
    const one = placeBid(newAuction(), 'rohan', 1000)
    assert.throws(() => placeBid(one, 'meera', 1099), isBidError('BID_TOO_LOW'))
    assert.equal(placeBid(one, 'meera', 1100).currentPrice, 1100)
  })

  it('reports the real minimum in the BID_TOO_LOW message', () => {
    assert.throws(() => placeBid(newAuction(), 'rohan', 900), /1000/, 'first bid: minimum is the starting price')
    const one = placeBid(newAuction(), 'rohan', 1000)
    assert.throws(() => placeBid(one, 'meera', 1050), /1100/, 'later bid: minimum is current + increment')
  })

  it('does not mutate the original auction', () => {
    const original = newAuction()
    placeBid(original, 'rohan', 1500)
    assert.equal(original.bids.length, 0)
    assert.equal(original.currentPrice, 1000)
  })

  it('blocks the seller from bidding', () => {
    assert.throws(() => placeBid(newAuction(), 'asha', 2000), isBidError('SELF_BIDDING_NOT_ALLOWED'))
  })

  it('still blocks bids on closed auctions at runtime', () => {
    const closed = closeAuction(newAuction())
    assert.throws(() => placeBid(untrusted<OpenAuction>(closed), 'rohan', 2000), isBidError('AUCTION_CLOSED'))
  })
})

describe('tryPlaceBid', () => {
  it('returns ok with the new auction when the bid is valid', () => {
    const result = tryPlaceBid(newAuction(), 'rohan', 1000)
    assert.equal(result.ok, true)
    if (result.ok) assert.equal(result.value.currentPrice, 1000)
  })

  it('returns the rejection instead of throwing', () => {
    const result = tryPlaceBid(newAuction(), 'asha', 5000)
    assert.equal(result.ok, false)
    if (!result.ok) assert.equal(result.error.code, 'SELF_BIDDING_NOT_ALLOWED')
  })
})

describe('closeAuction / getHighestBid / getWinner', () => {
  it('returns null when there are no bids', () => {
    assert.equal(getHighestBid(newAuction()), null)
    assert.equal(getWinner(closeAuction(newAuction())), null)
  })

  it('records the winning bid when closing', () => {
    let auction = newAuction()
    auction = placeBid(auction, 'rohan', 1000)
    auction = placeBid(auction, 'meera', 1200)
    const closed = closeAuction(auction)
    assert.equal(closed.status, 'CLOSED')
    assert.equal(closed.winningBid?.amount, 1200)
    assert.equal(getWinner(closed), 'meera')
  })

  it('still refuses to answer getWinner for an open auction at runtime', () => {
    assert.throws(() => getWinner(untrusted<ClosedAuction>(newAuction())))
  })

  it('cannot close twice', () => {
    const closed = closeAuction(newAuction())
    assert.throws(() => closeAuction(untrusted<OpenAuction>(closed)))
  })
})

describe('isOpen', () => {
  it('tells open and closed auctions apart', () => {
    const open: Auction = newAuction()
    const closed: Auction = closeAuction(newAuction())
    assert.equal(isOpen(open), true)
    assert.equal(isOpen(closed), false)
  })
})

describe('parseCreateAuctionInput', () => {
  const valid = { title: 'Leica M3', sellerId: 'asha', startingPrice: 1000, minimumIncrement: 100 }

  it('returns a typed input for valid data', () => {
    assert.deepEqual(parseCreateAuctionInput(valid), valid)
  })

  it('rejects anything that is not an object', () => {
    for (const bad of [null, undefined, 'auction', 42, []]) {
      assert.throws(() => parseCreateAuctionInput(bad), `input ${JSON.stringify(bad)}`)
    }
  })

  it('rejects wrong field types, including numbers sent as strings', () => {
    assert.throws(() => parseCreateAuctionInput({ ...valid, startingPrice: '1000' }))
    assert.throws(() => parseCreateAuctionInput({ ...valid, minimumIncrement: Number.NaN }))
    assert.throws(() => parseCreateAuctionInput({ ...valid, title: '   ' }))
    assert.throws(() => parseCreateAuctionInput({ ...valid, sellerId: 7 }))
  })

  it('names the bad field in the error message', () => {
    assert.throws(() => parseCreateAuctionInput({ ...valid, startingPrice: '1000' }), /startingPrice/)
  })
})
