import { useCallback, useEffect, useMemo, useState } from 'react'
import { useIde } from '../state/useIde'
import { errorsByPath as groupErrors } from '../state/selectors'
import { Explorer } from '../explorer/Explorer'
import { EditorArea } from '../editor/EditorArea'
import { BottomPanel } from '../panel/BottomPanel'
import { useResize } from '../panel/useResize'
import { DialogHost } from '../dialogs/DialogHost'
import { TopBar } from './TopBar'
import { StatusBar } from './StatusBar'
import { Toast } from './Toast'
import { BackendBanner } from './BackendBanner'
import { useShortcuts } from './useShortcuts'
import { useDiskOpen } from './useDiskOpen'
import { canPickFolder } from '../workspace/diskAccess'

const DEFAULT_PANEL_HEIGHT = 230

export function AppShell() {
  const [panelTab, setPanelTab] = useState('output')
  const [dialog, setDialog] = useState(null)
  const [edit, setEdit] = useState(null)
  const ide = useIde({ onFinish: setPanelTab })
  const { state, actions, compiler, toast, workspace } = ide
  const resize = useResize(DEFAULT_PANEL_HEIGHT)
  const openFromDisk = useDiskOpen(ide, setDialog)

  useShortcuts({ onRun: actions.runActive, onSave: actions.save, onStop: actions.stopRun })

  const errorsByPath = useMemo(() => groupErrors(state.results), [state.results])
  const activeResult = state.activePath ? state.results[state.activePath] : null
  const activeErrors = activeResult?.errors ?? []

  // New file, new folder and rename happen in the explorer row; the target folder opens so the row is visible.
  const startCreate = useCallback(
    (kind) => (parent) => {
      actions.expand(parent)
      setEdit({ kind, parent })
    },
    [actions],
  )

  // Closing a tab with unsaved edits asks first; the tab only closes after the answer.
  const requestClose = useCallback(
    (path) => (actions.isDirty(path) ? setDialog({ kind: 'discard', path }) : actions.closeTab(path)),
    [actions],
  )

  // Writing into a chosen folder can overwrite files, so it asks first; the download fallback never overwrites.
  const requestSaveProject = useCallback(
    () => (canPickFolder() ? setDialog({ kind: 'saveProject' }) : actions.downloadFolder('')),
    [actions],
  )

  useEffect(() => {
    const warn = (event) => {
      if (Object.keys(state.drafts).length > 0) event.preventDefault()
    }
    window.addEventListener('beforeunload', warn)
    return () => window.removeEventListener('beforeunload', warn)
  }, [state.drafts])

  if (workspace.isLoading) return <div className="shell" aria-busy="true" />

  return (
    <div className="shell">
      <TopBar ide={ide} onSaveProject={requestSaveProject} />
      <BackendBanner />
      <div className="main">
        <Explorer
          ide={ide}
          errorsByPath={errorsByPath}
          edit={edit}
          onNewFile={startCreate('file')}
          onNewFolder={startCreate('folder')}
          onRename={(path) => setEdit({ kind: 'rename', path })}
          onCancelEdit={() => setEdit(null)}
          onDelete={(path) => setDialog({ kind: 'delete', path })}
          onOpenDisk={openFromDisk}
        />
        <div className="center" style={{ gridTemplateRows: `minmax(0, 1fr) 7px ${resize.height}px` }}>
          <EditorArea
            ide={ide}
            errorsByPath={errorsByPath}
            onNewFile={() => startCreate('file')(actions.parentFor(state.selectedPath))}
            onClose={requestClose}
          />
          <div
            className="split"
            role="separator"
            aria-orientation="horizontal"
            aria-label="Resize results panel"
            tabIndex={0}
            {...resize.handlers}
          />
          <BottomPanel
            tab={panelTab}
            onTab={setPanelTab}
            errors={activeErrors}
            validation={activeResult}
            compileState={compiler.compileState}
            compiler={compiler}
            notify={actions.notify}
            onJump={(error) => actions.reveal(error.path, error.line, error.column)}
          />
        </div>
      </div>
      <StatusBar
        ide={ide}
        errorCount={activeErrors.length}
        checking={activeResult?.status === 'checking'}
      />
      <DialogHost dialog={dialog} ide={ide} onClose={() => setDialog(null)} />
      <Toast toast={toast} onClose={actions.dismissToast} />
    </div>
  )
}
