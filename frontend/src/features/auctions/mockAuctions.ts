import type { Auction, CategorySummary } from './types'

const hours = (n: number) => new Date(Date.now() + n * 3_600_000).toISOString()

export const mockCategories: CategorySummary[] = [
  { id: 1, name: 'Electronics', slug: 'electronics' },
  { id: 2, name: 'Collectibles', slug: 'collectibles' },
  { id: 3, name: 'Fashion', slug: 'fashion' },
  { id: 4, name: 'Home & Living', slug: 'home-living' },
  { id: 5, name: 'Art', slug: 'art' },
  { id: 6, name: 'Sports', slug: 'sports' },
]

const seller = { id: 1, displayName: 'Asha Seller' }
const cat = (slug: string) => mockCategories.find((c) => c.slug === slug) ?? null

function auction(partial: Partial<Auction> & Pick<Auction, 'id' | 'title' | 'currentPrice'>): Auction {
  return {
    description: null,
    coverImageUrl: `https://picsum.photos/seed/biddly-${partial.id}/800/600`,
    category: null,
    seller,
    startingPrice: partial.currentPrice,
    minimumIncrement: 500,
    startTime: hours(-1),
    endTime: hours(24),
    status: 'ACTIVE',
    serverTime: new Date().toISOString(),
    ...partial,
  }
}

export const mockAuctions: Auction[] = [
  auction({ id: 1, title: 'Vintage Leica M3 Camera', currentPrice: 45000, minimumIncrement: 1000, category: cat('collectibles'), endTime: hours(48), description: 'Fully serviced 1956 rangefinder with original leather case.' }),
  auction({ id: 2, title: 'Sony WH-1000XM5 Headphones', currentPrice: 18000, category: cat('electronics'), endTime: hours(5), description: 'Sealed box, Indian warranty valid for 12 months.' }),
  auction({ id: 3, title: 'Hand-painted Madhubani Canvas', currentPrice: 8500, minimumIncrement: 250, category: cat('art'), endTime: hours(72), description: 'Original 24x36 inch canvas by a Bihar folk artist.' }),
  auction({ id: 4, title: 'Signed Cricket Bat', currentPrice: 60000, minimumIncrement: 2000, category: cat('sports'), endTime: hours(0.66), description: 'English willow bat signed by the 2011 World Cup squad.' }),
  auction({ id: 5, title: 'Teak Wood Coffee Table', currentPrice: 22000, category: cat('home-living'), endTime: hours(24), description: 'Solid Burma teak, hand-finished, 4 ft x 2 ft.' }),
]
