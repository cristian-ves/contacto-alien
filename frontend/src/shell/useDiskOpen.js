import { useCallback } from 'react'
import { readDirectoryInput, readZipFile } from '../workspace/importExport'
import { hasNoFiles } from '../workspace/treeModel'
import { MSG } from '../messages'

// Reads what the user picked from disk and routes it: single files are added next to the selection, while a folder
// or .zip is a whole project and replaces the workspace (after a confirmation when files already exist).
// kind 'picker' is the folder picker of the File System Access API: its files keep their disk handles.
export function useDiskOpen(ide, setDialog) {
  const { tree, actions } = ide

  return useCallback(
    async (kind, fileList, targetFolder) => {
      try {
        if (kind === 'picker') {
          const result = await actions.pickProjectFolder()
          if (!result) return
          if (hasNoFiles(tree)) actions.replaceWorkspace(result)
          else setDialog({ kind: 'replace', result })
          return
        }
        if (kind === 'files') {
          actions.addFiles(await readDirectoryInput(fileList), targetFolder)
          return
        }
        const result = kind === 'zip' ? await readZipFile(fileList[0]) : await readDirectoryInput(fileList)
        if (hasNoFiles(tree)) actions.replaceWorkspace(result)
        else setDialog({ kind: 'replace', result })
      } catch (error) {
        console.error('Failed to read files from disk', error)
        actions.notify(kind === 'zip' ? MSG.zipFailed : MSG.readFailed, 'error')
      }
    },
    [tree, actions, setDialog],
  )
}
