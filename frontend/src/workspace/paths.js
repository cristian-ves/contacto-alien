export function dirname(path) {
  const slash = path.lastIndexOf('/')
  return slash === -1 ? '' : path.slice(0, slash)
}

export function basename(path) {
  return path.slice(path.lastIndexOf('/') + 1)
}

export function joinPath(parent, name) {
  return parent ? `${parent}/${name}` : name
}

export function isInside(path, folder) {
  return folder === '' || path === folder || path.startsWith(`${folder}/`)
}

// Rebases a path after a rename/move of oldPrefix to newPrefix; null if the path is outside it.
export function rebasePath(path, oldPrefix, newPrefix) {
  if (path === oldPrefix) return newPrefix
  if (path.startsWith(`${oldPrefix}/`)) return `${newPrefix}${path.slice(oldPrefix.length)}`
  return null
}
