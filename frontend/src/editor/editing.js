const INDENT = '    '
const PAIRS = { '(': ')', '[': ']', '{': '}', '"': '"', "'": "'" }
const CLOSERS = new Set(Object.values(PAIRS))

// execCommand keeps the browser's undo history; setRangeText is the fallback when it is unavailable.
function insert(textarea, text) {
  textarea.focus()
  if (document.execCommand('insertText', false, text)) return
  textarea.setRangeText(text, textarea.selectionStart, textarea.selectionEnd, 'end')
  textarea.dispatchEvent(new Event('input', { bubbles: true }))
}

// Editing conveniences that only touch the text: Tab inserts spaces, Enter keeps the indentation of the line above
// (and adds one level after "{"), and brackets/quotes can close themselves when the option is on.
export function handleEditorKey(event, { closeBrackets }) {
  if (event.ctrlKey || event.metaKey || event.altKey) return
  const textarea = event.target
  const { selectionStart: start, selectionEnd: end, value } = textarea

  if (event.key === 'Tab' && !event.shiftKey) {
    event.preventDefault()
    insert(textarea, INDENT)
    return
  }

  if (event.key === 'Enter') {
    event.preventDefault()
    const before = value.slice(0, start)
    const indent = /^[ \t]*/.exec(before.slice(before.lastIndexOf('\n') + 1))[0]
    const extra = before.trimEnd().endsWith('{') ? INDENT : ''
    insert(textarea, `\n${indent}${extra}`)
    return
  }

  if (!closeBrackets || start !== end) return

  if (CLOSERS.has(event.key) && value[start] === event.key) {
    event.preventDefault()
    textarea.setSelectionRange(start + 1, start + 1)
    return
  }

  if (PAIRS[event.key]) {
    event.preventDefault()
    insert(textarea, event.key + PAIRS[event.key])
    textarea.setSelectionRange(start + 1, start + 1)
  }
}
