import Button from '@mui/material/Button'
import Tooltip from '@mui/material/Tooltip'
import { ChevronRight, Download, Play, Save, Square } from 'lucide-react'
import { languageById } from '../api/languages'
import { basename, dirname } from '../workspace/paths'
import { DiskMenu } from './DiskMenu'
import { canSaveFile } from '../workspace/diskAccess'
import { MSG } from '../messages'

function Crumb({ path, language }) {
  if (!path) return null
  const folders = dirname(path) ? dirname(path).split('/') : []
  return (
    <div className="crumb">
      {folders.map((folder, index) => (
        <span key={index} className="crumb-part">
          {folder}
          <ChevronRight size={12} strokeWidth={2.2} />
        </span>
      ))}
      <strong>{basename(path)}</strong>
      {language && <span className="crumb-lang">{language.label}</span>}
    </div>
  )
}

export function TopBar({ ide, onSaveProject }) {
  const { state, actions, activeLanguage, compiler } = ide
  const { activePath } = state
  const { status } = compiler.compileState
  const starting = status === 'starting'
  const running = status === 'running'
  const canRun = activeLanguage === 'pig'
  const hasAssembly = Boolean(compiler.compileState.assembly)
  const checking = activePath !== null && state.results[activePath]?.status === 'checking'

  return (
    <header className="appbar">
      <div className="brand">
        <span className="mark" />
        {MSG.brand}
      </div>
      <Crumb path={activePath} language={languageById(activeLanguage)} />
      <span className="sp" />
      <Tooltip title={activePath && activePath in state.handles ? MSG.saveTipDisk : MSG.saveTip} placement="bottom">
        <span>
          <Button size="small" color="inherit" startIcon={<Save size={14} />} disabled={!activePath} onClick={() => actions.save()}>
            Save
          </Button>
        </span>
      </Tooltip>
      <DiskMenu
        hasFile={Boolean(activePath)}
        onSaveFile={() => (canSaveFile() ? actions.saveFileToDisk(activePath) : actions.downloadFile(activePath))}
        onSaveProject={onSaveProject}
      />
      <Tooltip title={activePath ? MSG.validateTip : MSG.validateDisabled} placement="bottom">
        <span>
          <Button size="small" color="inherit" disabled={!activePath || checking} onClick={actions.validateNow}>
            Validate
          </Button>
        </span>
      </Tooltip>
      <Tooltip title={hasAssembly ? MSG.downloadAsmTip : MSG.downloadAsmDisabled} placement="bottom">
        <span>
          <Button
            size="small"
            variant="outlined"
            color="secondary"
            startIcon={<Download size={14} />}
            disabled={!hasAssembly}
            onClick={actions.downloadAssembly}
          >
            Download .s
          </Button>
        </span>
      </Tooltip>
      {running ? (
        <Tooltip key="stop" title={MSG.stopTip} placement="bottom">
          <span>
            <Button
              variant="contained"
              color="error"
              startIcon={<Square size={14} fill="currentColor" />}
              onClick={actions.stopRun}
            >
              {MSG.stopLabel}
              <kbd>Esc</kbd>
            </Button>
          </span>
        </Tooltip>
      ) : (
        <Tooltip key="run" title={canRun ? MSG.runTip(basename(activePath)) : MSG.runDisabled} placement="bottom">
          <span>
            <Button
              variant="contained"
              color="primary"
              className={starting ? 'busy' : ''}
              disabled={!canRun}
              aria-busy={starting}
              startIcon={starting ? <span className="spinner" aria-hidden="true" /> : <Play size={16} fill="currentColor" />}
              onClick={actions.runActive}
            >
              {starting ? 'Compiling' : 'Compile & Run'}
              {!starting && <kbd>Ctrl Enter</kbd>}
            </Button>
          </span>
        </Tooltip>
      )}
    </header>
  )
}
