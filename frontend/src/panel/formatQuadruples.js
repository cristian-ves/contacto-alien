const NONE = '-'

// Text of one quadruple. The backend already sends a printable "text" field; the rebuild from op and operands is the
// fallback if it is missing, written in the same three-address notation used in the Project 1 statement.
function formatOne(quad) {
  if (typeof quad === 'string') return quad
  if (typeof quad.text === 'string') return quad.text
  const parts = [quad.op, quad.arg1, quad.arg2, quad.result].map((part) => (part == null ? NONE : String(part)))
  return parts.join(' ')
}

// Pure: quadruples (objects or strings) in, array of lines out.
export function formatQuadruples(quadruples) {
  return (quadruples ?? []).map(formatOne)
}

export function isLabelLine(line) {
  return /^[A-Za-z_][\w$.]*:$/.test(line) && !line.startsWith('func ')
}
