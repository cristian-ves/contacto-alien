import { useCallback, useMemo, useReducer, useState } from 'react'
import { useWorkspace } from '../workspace/useWorkspace'
import { findNodeByPath } from '../workspace/treeModel'
import { basename, dirname, joinPath } from '../workspace/paths'
import {
  buildTreeFromFlatFiles,
  downloadFileContent,
  downloadProjectAsZip,
} from '../workspace/importExport'
import { detectLanguageFromFilename } from '../api/languages'
import { useValidation } from '../hooks/useValidation'
import { useCompile } from '../hooks/useCompile'
import { useDiskActions } from './useDiskActions'
import { ideReducer, initialIdeState } from './ideReducer'
import { MSG } from '../messages'

// Single place where the workspace tree, the open documents, validation and compilation come together.
// Components receive this object and stay free of business logic.
export function useIde({ onFinish }) {
  const workspace = useWorkspace()
  const { tree } = workspace
  const [state, dispatch] = useReducer(ideReducer, initialIdeState)
  const [toast, setToast] = useState(null)

  const notify = useCallback((message, tone = 'ok', action = null) => {
    setToast({ message, tone, action, id: Date.now() })
  }, [])
  const dismissToast = useCallback(() => setToast(null), [])

  const getContent = useCallback(
    (path) => state.drafts[path] ?? findNodeByPath(tree, path)?.content ?? '',
    [state.drafts, tree],
  )

  const { activePath } = state
  const activeLanguage = activePath ? detectLanguageFromFilename(activePath) : null

  const { validateNow } = useValidation({
    tree,
    activePath,
    getContent,
    enabled: state.options.validateOnType,
    dispatch,
  })

  const onFailure = useCallback(
    (message, retry) => notify(message, 'error', retry ? { label: 'Retry', run: retry } : null),
    [notify],
  )
  const compiler = useCompile({ tree, getContent, dispatch, onFinish, onFailure })

  const isFile = useCallback((path) => findNodeByPath(tree, path)?.type === 'file', [tree])
  const isDirty = useCallback((path) => path in state.drafts, [state.drafts])

  const openFile = useCallback(
    (path) => {
      if (isFile(path)) dispatch({ type: 'open', path })
    },
    [isFile],
  )

  const commitDraft = useCallback(
    (path) => {
      if (!(path in state.drafts)) return
      workspace.updateContent(path, state.drafts[path])
      dispatch({ type: 'saved', path })
    },
    [state.drafts, workspace],
  )

  const commitAllDrafts = useCallback(() => {
    Object.keys(state.drafts).forEach(commitDraft)
  }, [state.drafts, commitDraft])

  const { writeLinked, saveFileToDisk, saveProjectToDisk, pickProjectFolder } = useDiskActions({
    tree,
    handles: state.handles,
    dispatch,
    getContent,
    notify,
    commitDraft,
    commitAllDrafts,
  })

  // Ctrl+S: always keeps the browser copy; when the file is linked to a disk file it is written there too.
  const save = useCallback(
    async (path = activePath) => {
      if (!path) return
      commitDraft(path)
      const outcome = await writeLinked(path)
      if (outcome === 'written') notify(MSG.savedDisk(basename(path)))
      else if (outcome === 'failed') notify(MSG.diskWriteFailed(basename(path)), 'error')
      else notify(MSG.saved(basename(path)))
      if (path === activePath) validateNow()
    },
    [activePath, commitDraft, writeLinked, notify, validateNow],
  )

  const parentFor = useCallback(
    (path) => (findNodeByPath(tree, path)?.type === 'file' ? dirname(path) : path),
    [tree],
  )

  const createFile = useCallback(
    (parent, name) => {
      const path = joinPath(parent, name)
      workspace.createFile(parent, name)
      dispatch({ type: 'expand', path: parent })
      dispatch({ type: 'open', path })
      notify(MSG.created(name))
    },
    [workspace, notify],
  )

  const createFolder = useCallback(
    (parent, name) => {
      workspace.createFolder(parent, name)
      dispatch({ type: 'expand', path: parent })
      dispatch({ type: 'expand', path: joinPath(parent, name) })
      notify(MSG.created(name))
    },
    [workspace, notify],
  )

  const rename = useCallback(
    (path, newName) => {
      workspace.rename(path, newName)
      dispatch({ type: 'renamed', oldPath: path, newPath: joinPath(dirname(path), newName) })
      notify(MSG.renamed(newName))
    },
    [workspace, notify],
  )

  const remove = useCallback(
    (path) => {
      workspace.remove(path)
      dispatch({ type: 'removed', path })
      notify(MSG.deleted(basename(path)))
    },
    [workspace, notify],
  )

  // Disk files overwrite same-path files and drop their unsaved drafts, so the editor shows what was opened.
  const addFiles = useCallback(
    ({ files, ignored }, baseFolder = '') => {
      if (files.length === 0) {
        notify(MSG.nothingOpened, 'error')
        return
      }
      workspace.upsertFiles(files, baseFolder)
      files.forEach((file) => dispatch({ type: 'saved', path: joinPath(baseFolder, file.path) }))
      if (baseFolder) dispatch({ type: 'expand', path: baseFolder })
      notify(MSG.opened(files.length, ignored.length))
    },
    [workspace, notify],
  )

  const replaceWorkspace = useCallback(
    ({ files, ignored }) => {
      if (files.length === 0) {
        notify(MSG.nothingOpened, 'error')
        return
      }
      workspace.replaceTree(buildTreeFromFlatFiles(files))
      dispatch({ type: 'reset' })
      const linked = files.filter((file) => file.handle).map((file) => [file.path, file.handle])
      if (linked.length > 0) dispatch({ type: 'link', handles: Object.fromEntries(linked) })
      notify(MSG.opened(files.length, ignored.length))
    },
    [workspace, notify],
  )

  const downloadFile = useCallback(
    (path) => {
      downloadFileContent(basename(path), getContent(path))
      notify(MSG.downloaded(basename(path)))
    },
    [getContent, notify],
  )

  const downloadFolder = useCallback(
    async (path) => {
      const node = path ? findNodeByPath(tree, path) : tree
      const name = path ? `${basename(path)}.zip` : 'project.zip'
      await downloadProjectAsZip(node, name, { getContent, stripPrefix: path })
      notify(MSG.downloaded(name))
    },
    [tree, getContent, notify],
  )

  const downloadAssembly = useCallback(() => {
    const { assembly, asmName } = compiler.compileState
    if (!assembly) return
    downloadFileContent(asmName, assembly)
    notify(MSG.downloaded(asmName))
  }, [compiler.compileState, notify])

  const runActive = useCallback(() => {
    if (activeLanguage !== 'pig') {
      notify(MSG.runDisabled, 'info')
      return
    }
    compiler.run(activePath)
  }, [activeLanguage, activePath, compiler, notify])

  const actions = useMemo(
    () => ({
      openFile,
      save,
      createFile,
      createFolder,
      rename,
      remove,
      addFiles,
      replaceWorkspace,
      downloadFile,
      downloadFolder,
      downloadAssembly,
      runActive,
      stopRun: compiler.stop,
      validateNow,
      saveFileToDisk,
      saveProjectToDisk,
      pickProjectFolder,
      parentFor,
      isDirty,
      getContent,
      notify,
      dismissToast,
      activate: (path) => dispatch({ type: 'activate', path }),
      closeTab: (path) => dispatch({ type: 'close', path }),
      edit: (path, text) => dispatch({ type: 'edit', path, text }),
      select: (path) => dispatch({ type: 'select', path }),
      toggle: (path) => dispatch({ type: 'toggle', path }),
      expand: (path) => dispatch({ type: 'expand', path }),
      setCursor: (cursor) => dispatch({ type: 'cursor', cursor }),
      reveal: (path, line, column) => {
        dispatch({ type: 'open', path })
        dispatch({ type: 'reveal', path, line, column })
      },
      setOption: (name, value) => dispatch({ type: 'option', name, value }),
    }),
    [
      openFile, save, createFile, createFolder, rename, remove, addFiles, replaceWorkspace, downloadFile,
      downloadFolder, downloadAssembly, runActive, compiler.stop, validateNow, saveFileToDisk, saveProjectToDisk,
      pickProjectFolder, parentFor, isDirty, getContent, notify, dismissToast,
    ],
  )

  return { workspace, tree, state, activeLanguage, compiler, toast, actions }
}
