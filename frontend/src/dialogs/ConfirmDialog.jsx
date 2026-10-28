import Button from '@mui/material/Button'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogTitle from '@mui/material/DialogTitle'

// Confirmation with one destructive/primary action and an optional second action (for example "Save").
export function ConfirmDialog({ title, body, confirmLabel, destructive = true, secondary, onConfirm, onClose }) {
  return (
    <Dialog open onClose={onClose}>
      <DialogTitle>{title}</DialogTitle>
      <DialogContent>{body}</DialogContent>
      <DialogActions>
        <Button color="inherit" onClick={onClose}>
          Cancel
        </Button>
        {secondary && (
          <Button variant="outlined" color="secondary" onClick={secondary.run}>
            {secondary.label}
          </Button>
        )}
        <Button variant="contained" color={destructive ? 'error' : 'primary'} onClick={onConfirm} autoFocus>
          {confirmLabel}
        </Button>
      </DialogActions>
    </Dialog>
  )
}
