// Groups every error of every validation result by the file it belongs to (imported files included).
// Two results can report the same error of a shared import, so identical entries are listed once.
export function errorsByPath(results) {
  const byPath = new Map()
  const seen = new Set()
  for (const result of Object.values(results)) {
    for (const error of result.errors) {
      const key = `${error.path}:${error.line}:${error.column}:${error.message}`
      if (seen.has(key)) continue
      seen.add(key)
      if (!byPath.has(error.path)) byPath.set(error.path, [])
      byPath.get(error.path).push(error)
    }
  }
  return byPath
}
