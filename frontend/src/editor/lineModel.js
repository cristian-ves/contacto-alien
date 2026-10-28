// Pure helpers that turn text + classified tokens + errors into per-line segments for the editor's colored layer.

function rangesByLine(tokens) {
  const byLine = new Map()
  for (const token of tokens) {
    const pieces = (token.text ?? '').split('\n')
    pieces.forEach((piece, offset) => {
      if (!piece) return
      const line = token.line + offset
      const start = offset === 0 ? token.startColumn : 0
      if (!byLine.has(line)) byLine.set(line, [])
      byLine.get(line).push({ start, end: start + piece.length, cat: token.cat })
    })
  }
  for (const ranges of byLine.values()) ranges.sort((a, b) => a.start - b.start)
  return byLine
}

function paint(lineText, ranges) {
  const segments = []
  let cursor = 0
  for (const range of ranges) {
    const start = Math.max(range.start, cursor)
    const end = Math.min(range.end, lineText.length)
    if (start >= end) continue
    if (start > cursor) segments.push({ text: lineText.slice(cursor, start), cat: 'plain' })
    segments.push({ text: lineText.slice(start, end), cat: range.cat })
    cursor = end
  }
  if (cursor < lineText.length) segments.push({ text: lineText.slice(cursor), cat: 'plain' })
  return segments
}

function underline(segments, from, to) {
  const out = []
  let position = 0
  for (const segment of segments) {
    const a = position
    const b = position + segment.text.length
    position = b
    if (b <= from || a >= to) {
      out.push(segment)
      continue
    }
    const i = Math.max(from, a) - a
    const j = Math.min(to, b) - a
    if (i > 0) out.push({ ...segment, text: segment.text.slice(0, i) })
    out.push({ ...segment, text: segment.text.slice(i, j), squiggle: true })
    if (j < segment.text.length) out.push({ ...segment, text: segment.text.slice(j) })
  }
  return out
}

// The underline covers the token under the error column; if no token is there, the single character at it.
function errorSpan(lineText, ranges, column) {
  const index = column - 1
  const token = ranges.find((range) => index >= range.start && index < range.end)
  if (token) return [token.start, Math.min(token.end, lineText.length)]
  if (lineText.length === 0) return null
  const start = Math.min(index, lineText.length - 1)
  return [start, start + 1]
}

export function buildLineModel(text, tokens, errors) {
  const byLine = rangesByLine(tokens)
  const errorsByLine = new Map()
  for (const error of errors) {
    if (!errorsByLine.has(error.line)) errorsByLine.set(error.line, [])
    errorsByLine.get(error.line).push(error)
  }

  return text.split('\n').map((lineText, index) => {
    const number = index + 1
    const ranges = byLine.get(number) ?? []
    let segments = paint(lineText, ranges)
    const lineErrors = errorsByLine.get(number) ?? []
    for (const error of lineErrors) {
      const span = errorSpan(lineText, ranges, error.column)
      if (span) segments = underline(segments, span[0], span[1])
    }
    return { number, segments, hasError: lineErrors.length > 0 }
  })
}

// Position -> character offset in the text, clamped to the line, used when jumping to an error.
export function offsetOf(text, line, column) {
  const lines = text.split('\n')
  let offset = 0
  for (let i = 0; i < Math.min(line - 1, lines.length - 1); i += 1) offset += lines[i].length + 1
  const lineLength = lines[Math.min(line, lines.length) - 1].length
  return offset + Math.min(Math.max(column - 1, 0), lineLength)
}

export function positionOf(text, offset) {
  const before = text.slice(0, offset)
  const line = before.split('\n').length
  return { line, column: offset - before.lastIndexOf('\n') }
}
