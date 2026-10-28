export function sortedChildren(node) {
  return [...(node.children ?? [])].sort((a, b) => {
    if (a.type !== b.type) return a.type === 'folder' ? -1 : 1
    return a.name.localeCompare(b.name)
  })
}

// Flat, ordered list of the rows currently shown (collapsed folders hide their content).
export function visibleRows(root, expanded) {
  const rows = []
  const walk = (node, depth) => {
    for (const child of sortedChildren(node)) {
      rows.push({ path: child.path, type: child.type, depth })
      if (child.type === 'folder' && expanded[child.path]) walk(child, depth + 1)
    }
  }
  walk(root, 0)
  return rows
}
