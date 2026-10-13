import Card from '@mui/material/Card'
import CardContent from '@mui/material/CardContent'
import Skeleton from '@mui/material/Skeleton'

export function AuctionCardSkeleton() {
  return (
    <Card>
      <Skeleton variant="rectangular" sx={{ aspectRatio: '4 / 3', height: 'auto' }} />
      <CardContent>
        <Skeleton width="80%" height={28} />
        <Skeleton width="40%" height={36} />
      </CardContent>
    </Card>
  )
}
