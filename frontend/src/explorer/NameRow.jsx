import { useEffect, useRef, useState } from 'react'
import { ChevronRight, Folder } from 'lucide-react'
import { FileBadge } from '../components/FileBadge'
import { NO_AUTOFILL } from '../components/noAutofill'
import { MSG } from '../messages'

// A tree row with a text field, used to create a file or folder and to rename. Enter or losing focus confirms a valid
// name, Escape cancels, and an empty name cancels on blur. The reason for an invalid name shows under the row.
export function NameRow({ depth, initial = '', isFile, validate, onCommit, onCancel }) {
  const [name, setName] = useState(initial)
  const [touched, setTouched] = useState(false)
  const ref = useRef(null)
  const finished = useRef(false)
  const error = validate(name)

  // Focus on mount; a rename selects the base name so typing replaces it and keeps the extension.
  useEffect(() => {
    const input = ref.current
    if (!input) return
    input.focus()
    const dot = initial.lastIndexOf('.')
    input.setSelectionRange(0, isFile && dot > 0 ? dot : initial.length)
    // Only on the first render of the row.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  // The row unmounts right after a commit and fires blur once more: only the first outcome counts.
  function finish(action) {
    if (finished.current) return
    finished.current = true
    action()
  }

  function onKeyDown(event) {
    if (event.nativeEvent.isComposing) return
    if (event.key === 'Enter') {
      event.preventDefault()
      if (!name.trim()) finish(onCancel)
      else if (error) setTouched(true)
      else finish(() => onCommit(name.trim()))
    } else if (event.key === 'Escape') {
      // Marked as handled so the global Escape (stop the program) does not also fire.
      event.preventDefault()
      event.stopPropagation()
      finish(onCancel)
    }
  }

  function onBlur() {
    if (!name.trim() || error) finish(onCancel)
    else finish(() => onCommit(name.trim()))
  }

  const showError = touched && Boolean(error)

  return (
    <li role="none" className="name-row-item">
      <div className={`row editing${showError ? ' invalid' : ''}`} style={{ '--d': depth }}>
        {isFile ? (
          <>
            <span className="gap" />
            <FileBadge path={name} />
          </>
        ) : (
          <>
            <ChevronRight className="chev" size={12} strokeWidth={2.2} />
            <Folder className="fold" size={16} strokeWidth={1.8} />
          </>
        )}
        <input
          ref={ref}
          className="name-input"
          value={name}
          aria-label={isFile ? MSG.fileNameLabel : MSG.folderNameLabel}
          aria-invalid={showError}
          aria-describedby={showError ? 'name-row-error' : undefined}
          onChange={(event) => {
            setName(event.target.value)
            setTouched(true)
          }}
          onKeyDown={onKeyDown}
          onBlur={onBlur}
          {...NO_AUTOFILL}
        />
      </div>
      {showError && (
        <div id="name-row-error" className="name-error" role="alert" style={{ '--d': depth }}>
          {error}
        </div>
      )}
    </li>
  )
}
