import { closeAuction, createAuction, getWinner, placeBid } from './simulator.js'

let auction = createAuction({ title: 'Vintage Leica M3', sellerId: 'asha', startingPrice: 45000, minimumIncrement: 1000 })
console.log('Created:', auction.title, 'at', auction.currentPrice)

for (const [bidder, amount] of [['rohan', 45000], ['meera', 46000], ['rohan', 46500], ['rohan', 48000]]) {
  try {
    auction = placeBid(auction, bidder, amount)
    console.log(`✔ ${bidder} bid ${amount}`)
  } catch (error) {
    console.log(`✘ ${bidder} bid ${amount} rejected: [${error.code}] ${error.message}`)
  }
}

auction = closeAuction(auction)
console.log('Winner:', getWinner(auction), 'at', auction.currentPrice)
