import CssBaseline from '@mui/material/CssBaseline'
import { ThemeProvider } from '@mui/material/styles'
import { theme } from './theme/theme'
import { GlobalVars } from './theme/GlobalVars'
import { BackendStatusProvider } from './api/BackendStatus'
import { AppShell } from './shell/AppShell'

function App() {
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <GlobalVars />
      <BackendStatusProvider>
        <AppShell />
      </BackendStatusProvider>
    </ThemeProvider>
  )
}

export default App
