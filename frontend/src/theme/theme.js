import { createTheme } from '@mui/material/styles'
import { colors, fonts, shadows, FOCUS_RING } from './palette'

const TRANSITION = 'background-color .15s, border-color .15s, box-shadow .15s, color .15s'

const buttonSizes = {
  small: { padding: '5px 10px', fontSize: 13 },
  medium: { padding: '8px 16px', fontSize: 14 },
  large: { padding: '10px 20px', fontSize: 16 },
}

export const theme = createTheme({
  palette: {
    mode: 'dark',
    primary: { main: colors.green, dark: colors.greenHi, contrastText: colors.bg },
    secondary: { main: colors.blue },
    error: { main: colors.red, dark: colors.redHi, contrastText: colors.bg },
    warning: { main: colors.orange },
    info: { main: colors.blue },
    success: { main: colors.green },
    background: { default: colors.bg, paper: colors.panel },
    text: { primary: colors.text, secondary: colors.muted, disabled: colors.faint },
    divider: colors.line,
  },
  shape: { borderRadius: 4 },
  typography: {
    fontFamily: fonts.ui,
    fontSize: 14,
    button: { textTransform: 'none', fontWeight: 600 },
  },
  components: {
    MuiCssBaseline: {
      styleOverrides: { body: { backgroundColor: colors.bg, color: colors.text } },
    },
    MuiButtonBase: { defaultProps: { disableRipple: true } },
    MuiButton: {
      defaultProps: { disableElevation: true },
      styleOverrides: {
        root: {
          borderRadius: 4,
          lineHeight: 1,
          whiteSpace: 'nowrap',
          gap: 8,
          transition: TRANSITION,
          '&:focus-visible': { boxShadow: FOCUS_RING },
          '&.Mui-disabled': { opacity: 0.35 },
          '& .MuiButton-startIcon': { margin: 0 },
        },
        sizeSmall: buttonSizes.small,
        sizeMedium: buttonSizes.medium,
        sizeLarge: buttonSizes.large,
        outlinedSecondary: {
          borderColor: colors.blue,
          '&:hover': { backgroundColor: colors.panel, borderColor: colors.blue },
        },
        textInherit: {
          color: colors.muted,
          '&:hover': { backgroundColor: colors.panel, color: colors.text },
        },
        containedPrimary: { '&:hover': { backgroundColor: colors.greenHi } },
        containedError: { '&:hover': { backgroundColor: colors.redHi } },
      },
    },
    MuiIconButton: {
      styleOverrides: {
        root: {
          width: 28,
          height: 28,
          borderRadius: 4,
          color: colors.muted,
          transition: TRANSITION,
          '&:hover': { backgroundColor: colors.panel, color: colors.text },
          '&:focus-visible': { boxShadow: FOCUS_RING },
          '&.Mui-disabled': { opacity: 0.35 },
        },
      },
    },
    MuiTooltip: {
      defaultProps: { enterDelay: 200, enterNextDelay: 200, arrow: true },
      styleOverrides: {
        tooltip: {
          backgroundColor: colors.line,
          color: colors.text,
          fontSize: 12,
          lineHeight: 1.4,
          padding: '6px 10px',
          borderRadius: 4,
          boxShadow: shadows.md,
          maxWidth: 260,
        },
        arrow: { color: colors.line },
      },
    },
    MuiOutlinedInput: {
      styleOverrides: {
        root: {
          backgroundColor: colors.bg,
          borderRadius: 4,
          transition: TRANSITION,
          '& .MuiOutlinedInput-notchedOutline': { borderColor: colors.line },
          '&:hover .MuiOutlinedInput-notchedOutline': { borderColor: colors.lineHi },
          '&.Mui-focused': { boxShadow: FOCUS_RING },
          '&.Mui-focused .MuiOutlinedInput-notchedOutline': { borderColor: colors.green, borderWidth: 1 },
          '&.Mui-error': { backgroundColor: colors.redField },
          '&.Mui-error .MuiOutlinedInput-notchedOutline': { borderColor: colors.red },
        },
        input: { padding: '8px 12px', fontSize: 14, lineHeight: 1.4 },
      },
    },
    MuiInputLabel: {
      styleOverrides: { root: { fontSize: 13, fontWeight: 500, color: colors.muted } },
    },
    MuiFormHelperText: {
      styleOverrides: { root: { marginLeft: 0, fontSize: 12.5 } },
    },
    MuiDialog: {
      styleOverrides: {
        paper: {
          backgroundImage: 'none',
          backgroundColor: colors.panel,
          border: `1px solid ${colors.line}`,
          borderRadius: 6,
          boxShadow: shadows.lg,
          width: 400,
          maxWidth: '90vw',
        },
      },
    },
    MuiDialogTitle: {
      styleOverrides: { root: { fontSize: 17, fontWeight: 600, padding: '20px 20px 0' } },
    },
    MuiDialogContent: {
      styleOverrides: { root: { padding: '14px 20px 0', color: colors.muted } },
    },
    MuiDialogActions: {
      styleOverrides: { root: { padding: '14px 20px 20px', gap: 8, '& > :not(style) ~ :not(style)': { marginLeft: 0 } } },
    },
    MuiMenu: {
      styleOverrides: {
        paper: {
          backgroundImage: 'none',
          backgroundColor: colors.panel,
          border: `1px solid ${colors.line}`,
          borderRadius: 6,
          boxShadow: shadows.lg,
          minWidth: 210,
        },
        list: { padding: 4 },
      },
    },
    MuiPopover: {
      styleOverrides: {
        paper: {
          backgroundImage: 'none',
          backgroundColor: colors.panel,
          border: `1px solid ${colors.line}`,
          borderRadius: 6,
          boxShadow: shadows.lg,
        },
      },
    },
    MuiMenuItem: {
      styleOverrides: {
        root: {
          gap: 12,
          justifyContent: 'space-between',
          padding: '7px 10px',
          borderRadius: 4,
          fontSize: 13.5,
          color: colors.soft,
          transition: 'background-color .12s',
          '&:hover, &.Mui-focusVisible': { backgroundColor: colors.line, color: colors.text },
        },
      },
    },
    MuiCheckbox: {
      defaultProps: { disableRipple: true },
      styleOverrides: {
        root: {
          padding: 0,
          marginTop: 2,
          marginRight: 10,
          color: colors.line,
          '&:hover': { color: colors.lineHi },
          '&.Mui-checked': { color: colors.green },
          '&.Mui-focusVisible': { boxShadow: FOCUS_RING },
          '& .MuiSvgIcon-root': { width: 18, height: 18 },
        },
      },
    },
    MuiFormControlLabel: {
      styleOverrides: {
        root: { alignItems: 'flex-start', marginLeft: 0, marginRight: 0, color: colors.soft },
        label: { fontSize: 14 },
      },
    },
    MuiSnackbarContent: {
      styleOverrides: {
        root: {
          backgroundColor: colors.panel,
          color: colors.text,
          border: `1px solid ${colors.line}`,
          borderLeft: `3px solid ${colors.green}`,
          borderRadius: 4,
          boxShadow: shadows.md,
          padding: '6px 12px 6px 14px',
          fontSize: 14,
          '&.toast-error': { borderLeftColor: colors.red },
        },
      },
    },
  },
})
