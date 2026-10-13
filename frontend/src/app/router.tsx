import { createBrowserRouter } from 'react-router'

import { AppLayout } from '../components/layout/AppLayout'
import { AuctionDetailsPage } from '../pages/AuctionDetailsPage'
import { AuctionsPage } from '../pages/AuctionsPage'
import { CreateAuctionPage } from '../pages/CreateAuctionPage'
import { HomePage } from '../pages/HomePage'
import { NotFoundPage } from '../pages/NotFoundPage'

export const router = createBrowserRouter([
  {
    element: <AppLayout />,
    children: [
      { index: true, element: <HomePage /> },
      { path: 'auctions', element: <AuctionsPage /> },
      { path: 'auctions/new', element: <CreateAuctionPage /> },
      { path: 'auctions/:auctionId', element: <AuctionDetailsPage /> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
])
