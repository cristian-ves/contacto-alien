import Button from '@mui/material/Button'
import Snackbar from '@mui/material/Snackbar'
import { Check, TriangleAlert } from 'lucide-react'

const DURATION = { ok: 3000, info: 3500, error: 9000 }

// One notification at a time. Errors can carry an action (Retry) that reruns what failed.
export function Toast({ toast, onClose }) {
  if (!toast) return null
  const isError = toast.tone === 'error'
  const Icon = isError ? TriangleAlert : Check
  return (
    <Snackbar
      key={toast.id}
      open
      autoHideDuration={DURATION[toast.tone] ?? DURATION.ok}
      onClose={(_event, reason) => reason !== 'clickaway' && onClose()}
      anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
      sx={{ bottom: { xs: 44 }, right: { xs: 16 } }}
      message={
        <span className="toast-line" role={isError ? 'alert' : 'status'}>
          <Icon size={16} className={isError ? 'bad' : 'ok'} />
          {toast.message}
        </span>
      }
      action={
        toast.action && (
          <Button
            size="small"
            color="inherit"
            onClick={() => {
              onClose()
              toast.action.run()
            }}
          >
            {toast.action.label}
          </Button>
        )
      }
      slotProps={{ content: { className: isError ? 'toast-error' : '' } }}
    />
  )
}
