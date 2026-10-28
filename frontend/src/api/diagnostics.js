import { basename, joinPath } from '../workspace/paths'
import { findNodeByPath, listAllFilesFlat } from '../workspace/treeModel'
import { MSG } from '../messages'

// Tone names match the chip classes in styles/base.css.
export const PHASES = {
  LEXICAL: { label: 'Lexical', tone: 'warn' },
  SYNTACTIC: { label: 'Syntax', tone: 'vio' },
  SEMANTIC: { label: 'Semantic', tone: 'bad' },
  GENERATION: { label: 'Generation', tone: 'info' },
  INTERNAL: { label: 'Internal', tone: 'mute' },
}

export function phaseInfo(phase) {
  return PHASES[phase] ?? PHASES.INTERNAL
}

// Errors found inside an imported file arrive as "[Folder/File.z] message"; that prefix names the file relative
// to the folder the request keys were built from (base), so it is turned into a workspace path when the file exists.
const IMPORT_PREFIX = /^\[([^\]]+)\]\s*(.*)$/s

// Wording of the backend when an import has no file in the map it received.
const IMPORT_NOT_FOUND = 'Imported file not found: '

// Says where the workspace was searched and, when a file with that name sits elsewhere, where it is.
function explainMissingImport(key, base, tree) {
  const expected = joinPath(base, key)
  const elsewhere = listAllFilesFlat(tree)
    .map((file) => file.path)
    .filter((candidate) => candidate !== expected && basename(candidate) === basename(key))
  return MSG.importNotFound(key, expected, elsewhere.slice(0, 3))
}

// Columns are taken as 1-based for every phase (the backend is being unified to that), so no compensation here.
export function normalizeErrors(errors, ownerPath, tree, base) {
  return (errors ?? []).map((error, index) => {
    let path = ownerPath
    let message = error.message ?? ''
    const match = IMPORT_PREFIX.exec(message)
    if (match) {
      const candidate = joinPath(base, match[1])
      if (findNodeByPath(tree, candidate)?.type === 'file') {
        path = candidate
        message = match[2]
      }
    } else if (message.startsWith(IMPORT_NOT_FOUND)) {
      message = explainMissingImport(message.slice(IMPORT_NOT_FOUND.length).trim(), base, tree)
    }
    return {
      id: `${path}:${error.line}:${error.column}:${index}`,
      phase: error.phase,
      message,
      line: Math.max(1, error.line ?? 1),
      column: Math.max(1, error.column ?? 1),
      path,
    }
  })
}
