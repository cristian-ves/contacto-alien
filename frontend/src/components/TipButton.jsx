import IconButton from '@mui/material/IconButton'
import Tooltip from '@mui/material/Tooltip'

// Icon button with the shared tooltip; the span lets the tooltip show on a disabled button too.
export function TipButton({ label, icon: Icon, onClick, disabled = false, placement = 'bottom', ...rest }) {
  return (
    <Tooltip title={label} placement={placement}>
      <span>
        <IconButton aria-label={label} onClick={onClick} disabled={disabled} {...rest}>
          <Icon size={16} strokeWidth={1.9} />
        </IconButton>
      </span>
    </Tooltip>
  )
}
