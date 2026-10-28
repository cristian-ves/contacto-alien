import { useBackend } from '../api/useBackend'
import { languageById } from '../api/languages'
import { MSG } from '../messages'

const BACKEND_TEXT = { up: MSG.backendUp, down: MSG.backendDownShort, checking: MSG.backendChecking }

// Plain text like the rest of the bar; only the number takes color when there are errors.
function ErrorCount({ count, checking, running }) {
  if (running) return 'Compiling'
  if (checking) return 'Checking'
  if (count === 0) return MSG.noErrors
  return (
    <>
      <span className="bad-num">{count}</span> {count === 1 ? 'error' : 'errors'}
    </>
  )
}

export function StatusBar({ ide, errorCount, checking }) {
  const { state, activeLanguage, compiler, workspace } = ide
  const { status } = useBackend()
  const language = languageById(activeLanguage)
  const dirty = state.activePath !== null && state.activePath in state.drafts
  const hasDrafts = Object.keys(state.drafts).length > 0
  const onDisk = state.activePath !== null && state.activePath in state.handles
  const savedText = onDisk ? MSG.savedDiskStatus : MSG.savedBrowser

  return (
    <footer className="status">
      <span>{language ? language.label : 'No file open'}</span>
      {state.activePath && (
        <span className="num">
          Line {state.cursor.line}, column {state.cursor.column}
        </span>
      )}
      <span className="num">
        <ErrorCount count={errorCount} checking={checking} running={compiler.compileState.status === 'running'} />
      </span>
      <span className="sp" />
      <span className={dirty || hasDrafts ? 'warn-text' : ''}>
        {workspace.persistFailed ? MSG.persistFailed : dirty || hasDrafts ? MSG.unsaved : savedText}
      </span>
      <span>
        <i className={`light ${status}`} />
        {BACKEND_TEXT[status]}
      </span>
    </footer>
  )
}
