import GavelRounded from '@mui/icons-material/GavelRounded'
import Box from '@mui/material/Box'
import { useState } from 'react'

interface AuctionImageProps {
  src: string | null
  alt: string
  aspectRatio?: string
}

/** Falls back to a branded gradient when the image is missing or fails to load. */
export function AuctionImage({ src, alt, aspectRatio = '4 / 3' }: AuctionImageProps) {
  const [failed, setFailed] = useState(false)

  if (!src || failed) {
    return (
      <Box
        role="img"
        aria-label={alt}
        sx={{
          aspectRatio,
          display: 'grid',
          placeItems: 'center',
          color: 'common.white',
          background: 'linear-gradient(135deg, #6D4AFF 0%, #FF6B4A 100%)',
          '& svg': { fontSize: 48, opacity: 0.85 },
        }}
      >
        <GavelRounded />
      </Box>
    )
  }

  return (
    <Box
      component="img"
      src={src}
      alt={alt}
      loading="lazy"
      onError={() => setFailed(true)}
      sx={{ display: 'block', width: '100%', aspectRatio, objectFit: 'cover' }}
    />
  )
}
