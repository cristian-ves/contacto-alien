// Pure model of the output console: a list of lines that grows as text arrives. A line stays "open" while the
// program has not finished it (a prompt such as "> "), so what the user types is shown right after it.
// Text from another stream, an echo that cannot join it, or a system message always starts on a new line.

export const MAX_LINES = 3000

export const emptyConsole = { lines: [], hidden: 0, nextId: 1 }

const LABELS = { out: 'stdout', err: 'stderr', in: 'input', sys: 'system' }

export function sourceLabel(stream) {
  return LABELS[stream]
}

function stamp() {
  return new Date().toTimeString().slice(0, 8)
}

function closeLast(lines) {
  const last = lines[lines.length - 1]
  if (!last?.open) return lines
  return [...lines.slice(0, -1), { ...last, open: false }]
}

function withPart(line, part) {
  const parts = line.parts
  const tail = parts[parts.length - 1]
  const merged = tail && tail.kind === part.kind ? [...parts.slice(0, -1), { ...tail, text: tail.text + part.text }] : [...parts, part]
  return { ...line, parts: merged }
}

function trim(state) {
  if (state.lines.length <= MAX_LINES) return state
  const dropped = state.lines.length - MAX_LINES
  return { ...state, lines: state.lines.slice(dropped), hidden: state.hidden + dropped }
}

// Adds a piece of text to the open line of the same stream, or starts a new line.
function addPiece(state, stream, tone, part, open) {
  const last = state.lines[state.lines.length - 1]
  if (last?.open && last.stream === stream) {
    const lines = [...state.lines.slice(0, -1), { ...withPart(last, part), open }]
    return { ...state, lines }
  }
  const line = { id: state.nextId, ts: stamp(), stream, tone, parts: [part], open }
  return trim({ ...state, lines: [...closeLast(state.lines), line], nextId: state.nextId + 1 })
}

function write(state, stream, text) {
  const tone = stream === 'err' ? 'err' : 'out'
  const pieces = text.replace(/\r/g, '').split('\n')
  let next = state
  pieces.forEach((piece, index) => {
    const endsLine = index < pieces.length - 1
    if (piece === '' && !endsLine) return
    next = addPiece(next, stream, tone, { kind: 'text', text: piece }, !endsLine)
  })
  return next
}

// What the user typed: it completes the prompt left open by stdout, otherwise it is a line of its own.
function echo(state, text) {
  const last = state.lines[state.lines.length - 1]
  if (last?.open && last.stream === 'out') {
    const lines = [...state.lines.slice(0, -1), { ...withPart(last, { kind: 'echo', text }), open: false }]
    return { ...state, lines }
  }
  const line = { id: state.nextId, ts: stamp(), stream: 'in', tone: 'in', parts: [{ kind: 'echo', text }], open: false }
  return trim({ ...state, lines: [...closeLast(state.lines), line], nextId: state.nextId + 1 })
}

function system(state, text, tone) {
  const line = { id: state.nextId, ts: stamp(), stream: 'sys', tone, parts: [{ kind: 'text', text }], open: false }
  return trim({ ...state, lines: [...closeLast(state.lines), line], nextId: state.nextId + 1 })
}

export function consoleReducer(state, action) {
  switch (action.type) {
    case 'reset':
      return emptyConsole
    case 'write':
      return write(state, action.stream, action.text)
    case 'echo':
      return echo(state, action.text)
    case 'system':
      return system(state, action.text, action.tone ?? 'system')
    default:
      return state
  }
}

// True when the last line is unfinished output of the program, so the input belongs right after it.
export function hasOpenPrompt(state) {
  const last = state.lines[state.lines.length - 1]
  return Boolean(last?.open && last.stream === 'out')
}
