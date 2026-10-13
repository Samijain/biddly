import assert from 'node:assert/strict'
import { describe, it } from 'node:test'

import {
  BidRejectedError,
  closeAuction,
  createAuction,
  createIdGenerator,
  getHighestBid,
  getWinner,
  placeBid,
} from './simulator.js'

const newAuction = () =>
  createAuction({ title: 'Leica M3', sellerId: 'asha', startingPrice: 1000, minimumIncrement: 100 })

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

  it('rejects non-positive prices', () => {
    const notTodo = (e) => !e.message.startsWith('TODO')
    assert.throws(() => createAuction({ title: 'x', sellerId: 's', startingPrice: 0, minimumIncrement: 10 }), notTodo)
    assert.throws(() => createAuction({ title: 'x', sellerId: 's', startingPrice: 10, minimumIncrement: -1 }), notTodo)
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
    assert.throws(() => placeBid(one, 'meera', 1099), (e) => e instanceof BidRejectedError && e.code === 'BID_TOO_LOW')
    assert.equal(placeBid(one, 'meera', 1100).currentPrice, 1100)
  })

  it('does not mutate the original auction', () => {
    const original = newAuction()
    placeBid(original, 'rohan', 1500)
    assert.equal(original.bids.length, 0)
    assert.equal(original.currentPrice, 1000)
  })

  it('blocks the seller from bidding', () => {
    assert.throws(() => placeBid(newAuction(), 'asha', 2000), (e) => e.code === 'SELF_BIDDING_NOT_ALLOWED')
  })

  it('blocks bids on closed auctions', () => {
    const closed = closeAuction(newAuction())
    assert.throws(() => placeBid(closed, 'rohan', 2000), (e) => e.code === 'AUCTION_CLOSED')
  })
})

describe('getHighestBid / getWinner', () => {
  it('returns null when there are no bids', () => {
    assert.equal(getHighestBid(newAuction()), null)
    assert.equal(getWinner(closeAuction(newAuction())), null)
  })

  it('finds the winner only after closing', () => {
    let auction = newAuction()
    auction = placeBid(auction, 'rohan', 1000)
    auction = placeBid(auction, 'meera', 1200)
    assert.equal(getHighestBid(auction).bidderId, 'meera')
    assert.throws(() => getWinner(auction))
    assert.equal(getWinner(closeAuction(auction)), 'meera')
  })

  it('cannot close twice', () => {
    assert.throws(() => closeAuction(closeAuction(newAuction())))
  })
})
