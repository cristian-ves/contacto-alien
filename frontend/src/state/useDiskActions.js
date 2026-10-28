import { useCallback } from 'react'
import { basename } from '../workspace/paths'
import { hasNoFiles } from '../workspace/treeModel'
import {
  isPickerCancel,
  pickFolderHandle,
  pickSaveHandle,
  readFolderHandle,
  writeHandle,
  writeTreeToFolder,
} from '../workspace/diskAccess'
import { MSG } from '../messages'

// Saving to the real disk with file handles. A path with a handle is "linked": Ctrl+S writes straight to that file.
// commitDraft/commitAllDrafts move unsaved edits into the tree (and so into IndexedDB) after a successful disk write.
export function useDiskActions({ tree, handles, dispatch, getContent, notify, commitDraft, commitAllDrafts }) {
  // Writes to the linked file of path, if any. Resolves 'none' when there is no handle, so callers fall back to
  // the browser-only save.
  const writeLinked = useCallback(
    async (path) => {
      const handle = handles[path]
      if (!handle) return 'none'
      try {
        await writeHandle(handle, getContent(path))
        return 'written'
      } catch (error) {
        console.error('Failed to write to disk', error)
        return 'failed'
      }
    },
    [handles, getContent],
  )

  const saveFileToDisk = useCallback(
    async (path) => {
      if (!path) return
      try {
        const handle = await pickSaveHandle(basename(path))
        await writeHandle(handle, getContent(path))
        commitDraft(path)
        dispatch({ type: 'link', handles: { [path]: handle } })
        notify(MSG.savedDisk(basename(path)))
      } catch (error) {
        if (isPickerCancel(error)) return
        console.error('Failed to save file to disk', error)
        notify(MSG.diskSaveFailed, 'error')
      }
    },
    [getContent, commitDraft, dispatch, notify],
  )

  const saveProjectToDisk = useCallback(async () => {
    if (hasNoFiles(tree)) {
      notify(MSG.nothingToSave, 'info')
      return
    }
    try {
      const folder = await pickFolderHandle()
      const written = await writeTreeToFolder(folder, tree, getContent)
      commitAllDrafts()
      dispatch({ type: 'link', handles: Object.fromEntries(written.map((item) => [item.path, item.handle])) })
      notify(MSG.savedProject(written.length, folder.name))
    } catch (error) {
      if (isPickerCancel(error)) return
      console.error('Failed to save project to disk', error)
      notify(MSG.diskSaveFailed, 'error')
    }
  }, [tree, getContent, commitAllDrafts, dispatch, notify])

  // Returns what readFolderHandle found, or null when the picker was cancelled or failed (already reported).
  const pickProjectFolder = useCallback(async () => {
    try {
      return await readFolderHandle(await pickFolderHandle())
    } catch (error) {
      if (!isPickerCancel(error)) {
        console.error('Failed to read folder from disk', error)
        notify(MSG.diskOpenFailed, 'error')
      }
      return null
    }
  }, [notify])

  return { writeLinked, saveFileToDisk, saveProjectToDisk, pickProjectFolder }
}
