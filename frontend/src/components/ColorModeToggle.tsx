import DarkModeRounded from '@mui/icons-material/DarkModeRounded'
import LightModeRounded from '@mui/icons-material/LightModeRounded'
import IconButton from '@mui/material/IconButton'
import { useColorScheme } from '@mui/material/styles'
import Tooltip from '@mui/material/Tooltip'

export function ColorModeToggle() {
  const { mode, systemMode, setMode } = useColorScheme()
  if (!mode) return null

  const resolved = mode === 'system' ? systemMode : mode
  const next = resolved === 'dark' ? 'light' : 'dark'

  return (
    <Tooltip title={`Switch to ${next} mode`}>
      <IconButton onClick={() => setMode(next)} aria-label={`Switch to ${next} mode`}>
        {resolved === 'dark' ? <LightModeRounded /> : <DarkModeRounded />}
      </IconButton>
    </Tooltip>
  )
}
