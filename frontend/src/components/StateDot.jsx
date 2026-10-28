import Tooltip from '@mui/material/Tooltip'
import { MSG } from '../messages'

// Colored dot next to a file name. kind is 'mod' (unsaved), 'bad' (has errors) or 'disk' (written to the disk).
export function StateDot({ kind }) {
  const label = MSG.dotLabels[kind]
  return (
    <Tooltip title={label} placement="top">
      <span className={`dot ${kind}`} role="img" aria-label={label} />
    </Tooltip>
  )
}
