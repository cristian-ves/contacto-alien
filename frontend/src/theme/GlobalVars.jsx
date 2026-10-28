import GlobalStyles from '@mui/material/GlobalStyles'
import { colors, fonts, shadows, FOCUS_RING } from './palette'

// Exposes the palette as CSS variables so the plain stylesheets read the same single source as the MUI theme.
const vars = {
  '--bg': colors.bg,
  '--editor': colors.editor,
  '--panel': colors.panel,
  '--line': colors.line,
  '--line-hi': colors.lineHi,
  '--text': colors.text,
  '--soft': colors.soft,
  '--muted': colors.muted,
  '--faint': colors.faint,
  '--dim': colors.dim,
  '--gutter': colors.gutter,
  '--green': colors.green,
  '--green-hi': colors.greenHi,
  '--green-bg': colors.greenBg,
  '--blue': colors.blue,
  '--blue-bg': colors.blueBg,
  '--orange': colors.orange,
  '--orange-bg': colors.orangeBg,
  '--red': colors.red,
  '--red-bg': colors.redBg,
  '--field-error': colors.redField,
  '--violet': colors.violet,
  '--violet-soft': colors.violetSoft,
  '--violet-bg': colors.violetBg,
  '--teal': colors.teal,
  '--code-text': colors.code,
  '--ui': fonts.ui,
  '--code': fonts.code,
  '--focus': FOCUS_RING,
  '--sh-md': shadows.md,
  '--sh-lg': shadows.lg,
}

export function GlobalVars() {
  return <GlobalStyles styles={{ ':root': vars }} />
}
