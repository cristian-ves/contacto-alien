import { useEffect, useRef } from 'react'
import { NO_AUTOFILL } from '../components/noAutofill'
import { MSG } from '../messages'

// The line where the user types the program's input: Enter sends it, Ctrl+D closes the input. It takes focus
// when it appears, which happens whenever the program starts waiting at a new place.
export function ConsoleInput({ value, onChange, onSubmit, onEnd, placeholder }) {
  const ref = useRef(null)

  useEffect(() => {
    const field = ref.current
    field?.focus()
    field?.setSelectionRange(field.value.length, field.value.length)
  }, [])

  function onKeyDown(event) {
    if (event.nativeEvent.isComposing) return
    if (event.key === 'Enter' && !event.ctrlKey && !event.metaKey) {
      event.preventDefault()
      onSubmit()
    } else if (event.key.toLowerCase() === 'd' && event.ctrlKey) {
      event.preventDefault()
      onEnd()
    }
  }

  return (
    <input
      ref={ref}
      className="console-input"
      aria-label={MSG.consoleInput}
      value={value}
      placeholder={placeholder}
      onChange={(event) => onChange(event.target.value)}
      onKeyDown={onKeyDown}
      {...NO_AUTOFILL}
    />
  )
}
