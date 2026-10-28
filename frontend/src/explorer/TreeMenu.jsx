import Menu from '@mui/material/Menu'
import MenuItem from '@mui/material/MenuItem'
import Divider from '@mui/material/Divider'

// Context menu of the explorer. target is the node under the pointer, or null for the empty area (workspace root).
export function TreeMenu({ position, target, onClose, onNewFile, onNewFolder, onRename, onDownload, onDelete }) {
  const run = (action) => () => {
    onClose()
    action()
  }
  return (
    <Menu
      open={Boolean(position)}
      onClose={onClose}
      // The inline name field takes focus right after the menu closes; restoring focus would blur it and cancel.
      disableRestoreFocus
      anchorReference="anchorPosition"
      anchorPosition={position ?? undefined}
    >
      <MenuItem onClick={run(onNewFile)}>New file</MenuItem>
      <MenuItem onClick={run(onNewFolder)}>New folder</MenuItem>
      {target && <MenuItem onClick={run(onRename)}>Rename<small className="hint">F2</small></MenuItem>}
      <MenuItem onClick={run(onDownload)}>{target ? 'Download' : 'Download project'}</MenuItem>
      {target && <Divider sx={{ my: 0.5 }} />}
      {target && (
        <MenuItem className="danger" onClick={run(onDelete)} sx={{ color: 'error.main' }}>
          Delete<small className="hint">Del</small>
        </MenuItem>
      )}
    </Menu>
  )
}
