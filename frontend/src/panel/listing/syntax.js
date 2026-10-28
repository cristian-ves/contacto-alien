// Own coloring for the generated texts. Each tokenizer is a small scanner that splits one line into
// { text, kind } pieces; the kind picks the CSS class (.lx-<kind>). Nothing here changes the text.

const QUAD_KEYWORDS = new Set([
  'func', 'end', 'call', 'param', 'recv', 'return', 'goto', 'if_false', 'if_true', 'new', 'concat', 'vtable',
  'halt', 'print_str', 'print_int', 'print_float', 'print_nl', 'int_to_str', 'float_to_str', 'read_line',
])

// Words of the form t12 are compiler temporaries; @name is a global; L3 and .L3 are jump targets.
const QUAD_RULES = [
  [/\s+/y, 'space'],
  [/"(?:[^"\\]|\\.)*"?/y, 'str'],
  [/#-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?/y, 'num'],
  [/-?\d+(?:\.\d+)?/y, 'num'],
  [/@[A-Za-z_$][\w$]*/y, 'glob'],
  [/\.?L\d+\b/y, 'label'],
  [/t\d+\b/y, 'tmp'],
  [/[A-Za-z_$][\w$]*#\d+/y, 'type'],
  [/[A-Za-z_$][\w$.]*/y, 'word'],
  [/[=<>!+\-*/%&|^]+\.?/y, 'op'],
  [/./y, 'punct'],
]

function scan(text, rules, classify) {
  const pieces = []
  let position = 0
  while (position < text.length) {
    let matched = false
    for (const [pattern, kind] of rules) {
      pattern.lastIndex = position
      const hit = pattern.exec(text)
      if (!hit) continue
      pieces.push({ text: hit[0], kind: classify ? classify(hit[0], kind, pieces) : kind })
      position += hit[0].length
      matched = true
      break
    }
    if (!matched) {
      pieces.push({ text: text[position], kind: 'punct' })
      position += 1
    }
  }
  return pieces
}

// A line that is only a name and a colon defines a label ("L1:", "func pig_main:" is a function header instead).
const LABEL_LINE = /^\s*[A-Za-z_.$][\w$.]*:\s*$/

export function tokenizeQuad(line) {
  if (LABEL_LINE.test(line)) return [{ text: line, kind: 'label' }]
  return scan(line, QUAD_RULES, (text, kind, before) => {
    if (kind !== 'word') return kind
    if (QUAD_KEYWORDS.has(text)) return 'kw'
    // The name a "func x:" header or a call points at is a function; anything else is a variable or field.
    const last = before.filter((piece) => piece.kind !== 'space').at(-1)
    return last && (last.text === 'func' || last.text === 'call' || last.text === '*') ? 'fn' : 'name'
  })
}

// RISC-V register names: x0-x31, ABI names and the floating-point ones.
const REGISTER = /^(?:x(?:[12]?\d|3[01])|zero|ra|sp|gp|tp|fp|t[0-6]|s(?:\d|1[01])|a[0-7]|f(?:t(?:\d|1[01])|s(?:\d|1[01])|a[0-7]))$/

const ASM_OPERAND_RULES = [
  [/\s+/y, 'space'],
  [/#.*/y, 'comment'],
  [/"(?:[^"\\]|\\.)*"?/y, 'str'],
  [/-?0x[\da-fA-F]+\b/y, 'imm'],
  [/-?\d+\b/y, 'imm'],
  [/[A-Za-z_.$][\w$.]*/y, 'sym'],
  [/./y, 'punct'],
]

const ASM_LABEL = /^(\s*)([A-Za-z_.$][\w$.]*:)(.*)$/s
const ASM_STATEMENT = /^(\s*)(\S+)(.*)$/s

function operands(text) {
  return scan(text, ASM_OPERAND_RULES, (word, kind) => (kind === 'sym' && REGISTER.test(word) ? 'reg' : kind))
}

export function tokenizeAsm(line) {
  if (line.trim() === '') return []
  if (line.trimStart().startsWith('#')) return [{ text: line, kind: 'comment' }]
  const label = ASM_LABEL.exec(line)
  if (label) {
    const [, indent, name, rest] = label
    return [{ text: indent, kind: 'space' }, { text: name, kind: 'label' }, ...operands(rest)]
  }
  const [, indent, head, rest] = ASM_STATEMENT.exec(line)
  return [
    { text: indent, kind: 'space' },
    { text: head, kind: head.startsWith('.') ? 'dir' : 'ins' },
    ...operands(rest),
  ]
}
