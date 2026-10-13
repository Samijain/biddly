import { alpha, createTheme } from '@mui/material/styles'
import type {} from '@mui/material/themeCssVarsAugmentation'

const brand = {
  violet: '#6D4AFF',
  coral: '#FF6B4A',
}

/**
 * One theme, two colour schemes. MUI writes the palette as CSS variables, so switching
 * light/dark is instant and does not re-render the React tree.
 */
export const theme = createTheme({
  cssVariables: { colorSchemeSelector: 'class' },
  colorSchemes: {
    light: {
      palette: {
        primary: { main: brand.violet },
        secondary: { main: brand.coral },
        background: { default: '#F6F5FB', paper: '#FFFFFF' },
        text: { primary: '#16131F', secondary: '#5E5A6E' },
        divider: alpha('#16131F', 0.08),
      },
    },
    dark: {
      palette: {
        primary: { main: '#8C73FF' },
        secondary: { main: '#FF8466' },
        background: { default: '#0E0C14', paper: '#17141F' },
        text: { primary: '#F4F2FA', secondary: '#A9A4BA' },
        divider: alpha('#FFFFFF', 0.08),
      },
    },
  },
  shape: { borderRadius: 14 },
  typography: {
    fontFamily: '"Inter Variable", "Inter", system-ui, -apple-system, "Segoe UI", sans-serif',
    h1: { fontWeight: 800, letterSpacing: '-0.035em' },
    h2: { fontWeight: 800, letterSpacing: '-0.03em' },
    h3: { fontWeight: 700, letterSpacing: '-0.025em' },
    h4: { fontWeight: 700, letterSpacing: '-0.02em' },
    h5: { fontWeight: 700 },
    h6: { fontWeight: 650 },
    button: { textTransform: 'none', fontWeight: 600 },
    overline: { fontWeight: 700, letterSpacing: '0.12em' },
  },
  components: {
    MuiButton: {
      defaultProps: { disableElevation: true },
      styleOverrides: { root: { borderRadius: 999, paddingInline: 20 } },
    },
    MuiCard: {
      defaultProps: { elevation: 0 },
      styleOverrides: {
        root: ({ theme }) => ({
          border: `1px solid ${theme.vars.palette.divider}`,
          transition: 'transform 180ms ease, box-shadow 180ms ease, border-color 180ms ease',
        }),
      },
    },
    MuiChip: { styleOverrides: { root: { fontWeight: 600 } } },
    MuiTextField: { defaultProps: { size: 'small' } },
  },
})
