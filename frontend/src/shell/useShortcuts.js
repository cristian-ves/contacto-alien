import { useEffect, useRef } from 'react'

// Global shortcuts: Ctrl/Cmd+Enter runs the program, Esc stops it, Ctrl/Cmd+S saves the active file.
// Handlers go through a ref so the listener is attached once and always calls the latest ones.
export function useShortcuts({ onRun, onSave, onStop }) {
  const handlers = useRef({ onRun, onSave, onStop })
  useEffect(() => {
    handlers.current = { onRun, onSave, onStop }
  })

  useEffect(() => {
    function onKeyDown(event) {
      // A dialog or menu that handled Escape marks the event; only a bare Escape reaches the program.
      if (event.key === 'Escape' && !event.defaultPrevented && !event.ctrlKey && !event.metaKey && !event.altKey) {
        handlers.current.onStop()
        return
      }
      if (!(event.ctrlKey || event.metaKey) || event.altKey) return
      if (event.key === 'Enter') {
        event.preventDefault()
        handlers.current.onRun()
      } else if (event.key.toLowerCase() === 's') {
        event.preventDefault()
        handlers.current.onSave()
      }
    }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [])
}
