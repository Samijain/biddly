import TimerOutlined from '@mui/icons-material/TimerOutlined'
import Chip from '@mui/material/Chip'

import { useNow } from '../../../hooks/useNow'
import { formatTimeLeft, timeLeft } from '../../../lib/format'

const ONE_HOUR = 3_600_000

interface CountdownProps {
  endTime: string
  size?: 'small' | 'medium'
}

/** The browser only *displays* time left. Whether a bid is in time is decided by the server. */
export function Countdown({ endTime, size = 'small' }: CountdownProps) {
  const now = useNow()
  const left = timeLeft(endTime, now)
  const urgent = left.totalMs > 0 && left.totalMs < ONE_HOUR

  return (
    <Chip
      size={size}
      icon={<TimerOutlined />}
      label={formatTimeLeft(left)}
      color={left.totalMs === 0 ? 'default' : urgent ? 'secondary' : 'default'}
      variant={urgent ? 'filled' : 'outlined'}
      sx={{ fontVariantNumeric: 'tabular-nums', backdropFilter: 'blur(8px)' }}
    />
  )
}
