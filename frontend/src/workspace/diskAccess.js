import { detectLanguageFromFilename } from '../api/languages'

// Thin wrapper over the File System Access API (Chromium browsers). Nothing here touches React or the tree.
const SKIPPED_FOLDERS = new Set(['node_modules'])

export function canSaveFile() {
  return typeof window !== 'undefined' && 'showSaveFilePicker' in window
}

export function canPickFolder() {
  return typeof window !== 'undefined' && 'showDirectoryPicker' in window
}

// Closing the picker without choosing throws AbortError; that is a cancel, not a failure.
export function isPickerCancel(error) {
  return error?.name === 'AbortError'
}

export async function writeHandle(handle, content) {
  const writable = await handle.createWritable()
  try {
    await writable.write(content)
  } finally {
    await writable.close()
  }
}

export function pickSaveHandle(suggestedName) {
  return window.showSaveFilePicker({ suggestedName })
}

export function pickFolderHandle() {
  return window.showDirectoryPicker({ mode: 'readwrite' })
}

// Reads the supported files of a folder recursively. Paths are relative to the picked folder, and each file keeps its
// handle so a later save can write back to the same file on disk.
export async function readFolderHandle(folder, prefix = '') {
  const files = []
  const ignored = []
  for await (const [name, entry] of folder.entries()) {
    const path = prefix ? `${prefix}/${name}` : name
    if (entry.kind === 'directory') {
      if (name.startsWith('.') || SKIPPED_FOLDERS.has(name)) continue
      const inner = await readFolderHandle(entry, path)
      files.push(...inner.files)
      ignored.push(...inner.ignored)
    } else if (detectLanguageFromFilename(name) === null) {
      ignored.push(path)
    } else {
      const file = await entry.getFile()
      files.push({ name, path, content: await file.text(), handle: entry })
    }
  }
  return { files, ignored }
}

// Writes a tree node's children into a folder handle, creating folders and files. Same-name files are overwritten and
// other files in the folder are left alone. Returns the { path, handle } of every file written.
export async function writeTreeToFolder(folder, node, getContent, written = []) {
  for (const child of node.children ?? []) {
    if (child.type === 'folder') {
      const inner = await folder.getDirectoryHandle(child.name, { create: true })
      await writeTreeToFolder(inner, child, getContent, written)
    } else {
      const handle = await folder.getFileHandle(child.name, { create: true })
      await writeHandle(handle, getContent(child.path))
      written.push({ path: child.path, handle })
    }
  }
  return written
}
