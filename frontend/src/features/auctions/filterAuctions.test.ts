import { filterAuctions } from './filterAuctions'
import { mockAuctions } from './mockAuctions'
import type { AuctionFilter } from './types'

const base: AuctionFilter = { search: '', categorySlug: null, sort: 'ENDING_SOON' }

// CHALLENGE 3: change `describe.skip` to `describe` once filterAuctions is implemented.
describe.skip('filterAuctions', () => {
  it('keeps everything for an empty search', () => {
    expect(filterAuctions(mockAuctions, base)).toHaveLength(mockAuctions.length)
  })

  it('matches title and description case-insensitively and trims the search', () => {
    expect(filterAuctions(mockAuctions, { ...base, search: '  LEICA ' }).map((a) => a.id)).toEqual([1])
    expect(filterAuctions(mockAuctions, { ...base, search: 'warranty' }).map((a) => a.id)).toEqual([2])
  })

  it('filters by category', () => {
    const result = filterAuctions(mockAuctions, { ...base, categorySlug: 'art' })
    expect(result.every((a) => a.category?.slug === 'art')).toBe(true)
  })

  it('sorts by ending soon', () => {
    const ends = filterAuctions(mockAuctions, base).map((a) => Date.parse(a.endTime))
    expect(ends).toEqual([...ends].sort((a, b) => a - b))
  })

  it('sorts by price high to low', () => {
    const prices = filterAuctions(mockAuctions, { ...base, sort: 'PRICE_HIGH' }).map((a) => a.currentPrice)
    expect(prices).toEqual([60000, 45000, 22000, 18000, 8500])
  })

  it('does not mutate the input array', () => {
    const input = [...mockAuctions]
    filterAuctions(input, { ...base, sort: 'PRICE_LOW' })
    expect(input).toEqual(mockAuctions)
  })
})
