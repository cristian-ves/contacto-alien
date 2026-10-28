// Facts the Assembly view derives from the generated text. The backend sends one string, so the structure is read
// from the markers the generator writes: a "# --- runtime" banner before the fixed routines, "# <quadruple>"
// comments before the instructions of each quadruple, and labels at the start of a line.

const RUNTIME_MARKER = /^#\s*-{8,}\s*runtime\s*$/
// The program's own data (strings, globals, class tables) is emitted after the runtime's data blocks.
const PROGRAM_DATA_LABEL = /^(?:str_\d+|class_table|vt_\d+|g_[\w$]+):/
const LABEL = /^([A-Za-z_][\w$.]*):/
const SECTION_DIRECTIVE = /^\s*\.(?:data|bss|section\s+\.rodata|rodata)\b/
const TEXT_DIRECTIVE = /^\s*\.text\b/

// Line ranges are [from, to) indexes into the lines array.
export function findRuntimeRange(lines) {
  const start = lines.findIndex((line) => RUNTIME_MARKER.test(line.trim()))
  if (start === -1) return null
  let tail = lines.findIndex((line, index) => index > start && PROGRAM_DATA_LABEL.test(line))
  if (tail === -1) return { from: start, to: lines.length }
  // Take the directives that open the program's data block (.section, .balign) with it.
  while (tail > start + 1 && lines[tail - 1].trim().startsWith('.')) tail -= 1
  return { from: start, to: tail }
}

// Named jump targets and functions for the search box. Local labels (.L...) are left out.
export function buildIndex(lines) {
  const entries = []
  let inCode = true
  lines.forEach((line, index) => {
    if (TEXT_DIRECTIVE.test(line)) inCode = true
    else if (SECTION_DIRECTIVE.test(line)) inCode = false
    const label = LABEL.exec(line)
    if (label) entries.push({ name: label[1], line: index, group: inCode ? 'Code' : 'Data' })
  })
  return entries
}

// The generator copies each quadruple as "# <text>" ahead of its instructions, in order, so walking both lists
// together gives every quadruple its assembly lines; limit is where the program's code ends. A quadruple with no comment gets no entry.
export function linkQuadruples(quads, lines, limit) {
  const links = new Map()
  const starts = []
  let cursor = 0
  quads.forEach((text, quadIndex) => {
    for (let index = cursor; index < limit; index += 1) {
      if (lines[index] === `# ${text}`) {
        starts.push({ quadIndex, from: index })
        cursor = index + 1
        return
      }
    }
  })
  starts.forEach((entry, position) => {
    const next = starts[position + 1]
    links.set(entry.quadIndex, { from: entry.from, to: next ? next.from : limit })
  })
  return links
}

// Rows to draw. With programOnly the runtime range collapses into one marker row; numbers stay the original ones.
export function visibleRows(lines, runtime, programOnly) {
  const rows = []
  lines.forEach((text, index) => {
    if (programOnly && runtime && index >= runtime.from && index < runtime.to) {
      if (index === runtime.from) rows.push({ gap: runtime.to - runtime.from, index })
      return
    }
    rows.push({ n: index + 1, index, text })
  })
  return rows
}
