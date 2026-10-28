import { useRef } from 'react'
import Menu from '@mui/material/Menu'
import MenuItem from '@mui/material/MenuItem'
import { canPickFolder } from '../workspace/diskAccess'

const DISK_KINDS = [
  { kind: 'files', label: 'Open files' },
  { kind: 'folder', label: 'Open folder' },
  { kind: 'zip', label: 'Open .zip project' },
]

// The "Open from disk" menu and the hidden file inputs behind it.
export function DiskOpenControls({ anchorEl, onClose, targetFolder, onOpenDisk }) {
  const inputs = { files: useRef(null), folder: useRef(null), zip: useRef(null) }

  return (
    <>
      <Menu open={Boolean(anchorEl)} anchorEl={anchorEl} onClose={onClose}>
        {DISK_KINDS.map(({ kind, label }) => (
          <MenuItem
            key={kind}
            onClick={() => {
              onClose()
              // The folder picker keeps disk handles so Ctrl+S writes back; browsers without it use the input.
              if (kind === 'folder' && canPickFolder()) onOpenDisk('picker', [], targetFolder)
              else inputs[kind].current?.click()
            }}
          >
            {label}
          </MenuItem>
        ))}
      </Menu>

      {DISK_KINDS.map(({ kind }) => (
        <input
          key={kind}
          ref={inputs[kind]}
          type="file"
          hidden
          multiple={kind === 'files'}
          accept={kind === 'files' ? '.pig,.y,.z' : kind === 'zip' ? '.zip' : undefined}
          {...(kind === 'folder' ? { webkitdirectory: '', directory: '' } : {})}
          onChange={(event) => {
            // The FileList is live: copy it before resetting the input or the files vanish during the async read.
            const picked = Array.from(event.target.files ?? [])
            event.target.value = ''
            if (picked.length > 0) onOpenDisk(kind, picked, targetFolder)
          }}
        />
      ))}
    </>
  )
}
