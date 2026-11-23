import { createBrowserRouter } from 'react-router'

import { AppLayout } from '../components/layout/AppLayout'
import { AuctionDetailsPage } from '../pages/AuctionDetailsPage'
import { AuctionsPage } from '../pages/AuctionsPage'
import { CreateAuctionPage } from '../pages/CreateAuctionPage'
import { EditAuctionPage } from '../pages/EditAuctionPage'
import { HomePage } from '../pages/HomePage'
import { NotFoundPage } from '../pages/NotFoundPage'
import { RegisterPage } from '../pages/RegisterPage'
import { SignInPage } from '../pages/SignInPage'

export const router = createBrowserRouter([
  {
    element: <AppLayout />,
    children: [
      { index: true, element: <HomePage /> },
      { path: 'auctions', element: <AuctionsPage /> },
      { path: 'auctions/new', element: <CreateAuctionPage /> },
      { path: 'auctions/:auctionId', element: <AuctionDetailsPage /> },
      { path: 'auctions/:auctionId/edit', element: <EditAuctionPage /> },
      { path: 'register', element: <RegisterPage /> },
      { path: 'sign-in', element: <SignInPage /> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
])
