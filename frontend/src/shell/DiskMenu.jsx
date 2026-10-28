import { useState } from 'react'
import Button from '@mui/material/Button'
import Menu from '@mui/material/Menu'
import MenuItem from '@mui/material/MenuItem'
import Tooltip from '@mui/material/Tooltip'
import { ChevronDown, HardDriveDownload } from 'lucide-react'
import { canPickFolder, canSaveFile } from '../workspace/diskAccess'
import { MSG } from '../messages'

// Saving to the real disk. With the File System Access API the items open the native pickers; without it
// (Firefox, Safari) the same menu offers plain downloads and the tooltip says why.
export function DiskMenu({ hasFile, onSaveFile, onSaveProject }) {
  const [anchor, setAnchor] = useState(null)
  const nativeFile = canSaveFile()
  const nativeFolder = canPickFolder()
  const supported = nativeFile && nativeFolder

  const choose = (action) => () => {
    setAnchor(null)
    action()
  }

  return (
    <>
      <Tooltip title={supported ? MSG.diskMenuTip : MSG.diskUnsupportedTip} placement="bottom">
        <span>
          <Button
            size="small"
            color="inherit"
            startIcon={<HardDriveDownload size={14} />}
            endIcon={<ChevronDown size={12} />}
            aria-haspopup="menu"
            aria-expanded={Boolean(anchor)}
            onClick={(event) => setAnchor(event.currentTarget)}
          >
            {supported ? 'Save to disk' : 'Download'}
          </Button>
        </span>
      </Tooltip>
      <Menu open={Boolean(anchor)} anchorEl={anchor} onClose={() => setAnchor(null)}>
        <MenuItem disabled={!hasFile} onClick={choose(onSaveFile)}>
          {nativeFile ? MSG.saveFileToDisk : MSG.downloadFile}
        </MenuItem>
        <MenuItem onClick={choose(onSaveProject)}>{nativeFolder ? MSG.saveProjectToDisk : MSG.downloadProject}</MenuItem>
      </Menu>
    </>
  )
}
