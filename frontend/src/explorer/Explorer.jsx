import { useMemo, useRef, useState } from 'react'
import Button from '@mui/material/Button'
import useMediaQuery from '@mui/material/useMediaQuery'
import { CircleHelp, Download, FilePlus, FolderPlus, Folder, Upload } from 'lucide-react'
import { TipButton } from '../components/TipButton'
import { DiskOpenControls } from './DiskOpenControls'
import { LegendDialog } from './LegendDialog'
import { NameRow } from './NameRow'
import { TreeMenu } from './TreeMenu'
import { TreeNode } from './TreeNode'
import { sortedChildren, visibleRows } from './visibleRows'
import { basename, dirname } from '../workspace/paths'
import { findNodeByPath } from '../workspace/treeModel'
import { validateName } from '../workspace/nameRules'
import { MSG } from '../messages'

const FOLDER_MOTION_MS = 150

// Explorer pane: tree, header actions, context menu and keyboard navigation. Creating and renaming happen in a row
// of the tree itself; edit is { kind: 'file' | 'folder', parent } or { kind: 'rename', path } or null. Everything
// else that changes data is delegated through the on* props, so this component only decides what the user asked for.
export function Explorer({ ide, errorsByPath, edit, onNewFile, onNewFolder, onRename, onCancelEdit, onDelete, onOpenDisk }) {
  const { tree, state, actions } = ide
  const { expanded, activePath, selectedPath, drafts, handles } = state
  const treeRef = useRef(null)
  const [menu, setMenu] = useState(null)
  const [diskAnchor, setDiskAnchor] = useState(null)
  const [legendOpen, setLegendOpen] = useState(false)
  const reduceMotion = useMediaQuery('(prefers-reduced-motion: reduce)')

  const rows = useMemo(() => visibleRows(tree, expanded), [tree, expanded])
  const tabStop = rows.some((row) => row.path === selectedPath) ? selectedPath : (rows[0]?.path ?? null)
  const targetFolder = actions.parentFor(selectedPath)
  const targetText = targetFolder ? `${targetFolder}/` : MSG.workspaceRootTarget

  function editRow(depth, { parent, path }) {
    const isRename = path !== undefined
    const isFile = isRename ? findNodeByPath(tree, path)?.type === 'file' : edit.kind === 'file'
    const parentPath = isRename ? dirname(path) : parent
    return (
      <NameRow
        key="name-row"
        depth={depth}
        initial={isRename ? basename(path) : ''}
        isFile={isFile}
        validate={(name) => validateName({ name, isFile, parentPath, tree, ownPath: path })}
        onCancel={onCancelEdit}
        onCommit={(name) => {
          onCancelEdit()
          if (isRename) {
            if (name !== basename(path)) actions.rename(path, name)
          } else if (isFile) actions.createFile(parent, name)
          else actions.createFolder(parent, name)
        }}
      />
    )
  }

  const creatingAt = (folder) => edit && edit.kind !== 'rename' && edit.parent === folder

  const ctx = {
    expanded,
    activePath,
    selectedPath,
    tabStop,
    dirty: new Set(Object.keys(drafts)),
    errored: new Set(errorsByPath.keys()),
    onDisk: new Set(Object.keys(handles)),
    motion: reduceMotion ? 0 : FOLDER_MOTION_MS,
    createRowFor: (folder, depth) => (creatingAt(folder) ? editRow(depth, { parent: folder }) : null),
    renameRowFor: (path, depth) => (edit?.kind === 'rename' && edit.path === path ? editRow(depth, { path }) : null),
    onActivate: (node) => {
      actions.select(node.path)
      if (node.type === 'folder') actions.toggle(node.path)
      else actions.openFile(node.path)
    },
    onContext: (event, node) => {
      event.preventDefault()
      event.stopPropagation()
      actions.select(node.path)
      setMenu({ position: { top: event.clientY, left: event.clientX }, path: node.path })
    },
  }

  const focusRow = (path) => treeRef.current?.querySelector(`[data-path="${CSS.escape(path)}"]`)?.focus()

  function onKeyDown(event) {
    const path = event.target.dataset?.path
    if (path === undefined) return
    const index = rows.findIndex((row) => row.path === path)
    const row = rows[index]
    if (event.key === 'ArrowDown') focusRow(rows[Math.min(index + 1, rows.length - 1)].path)
    else if (event.key === 'ArrowUp') focusRow(rows[Math.max(index - 1, 0)].path)
    else if (event.key === 'ArrowRight' && row.type === 'folder') {
      if (!expanded[path]) actions.toggle(path)
      else if (rows[index + 1]?.depth > row.depth) focusRow(rows[index + 1].path)
    } else if (event.key === 'ArrowLeft') {
      if (row.type === 'folder' && expanded[path]) actions.toggle(path)
      else if (row.depth > 0) focusRow(dirname(path))
    } else if (event.key === 'F2') onRename(path)
    else if (event.key === 'Delete') onDelete(path)
    else return
    event.preventDefault()
  }

  // Escape in the explorer drops the selection so the next file or folder goes to the root. It is marked as handled
  // so it does not also stop a running program.
  function onEscape(event) {
    if (event.key !== 'Escape' || selectedPath === '' || event.defaultPrevented) return
    event.preventDefault()
    actions.select('')
  }

  const menuTarget = menu ? menu.path : null
  const download = (path) => (actions.parentFor(path) === path ? actions.downloadFolder(path) : actions.downloadFile(path))
  const menuParent = menuTarget === null ? '' : actions.parentFor(menuTarget)
  const showEmpty = rows.length === 0 && !creatingAt('')

  return (
    <aside className="explorer" aria-label={MSG.explorer} onKeyDown={onEscape}>
      <div className="pane-h">
        {MSG.explorer}
        <div className="acts">
          <TipButton label={MSG.newIn('file', targetText)} icon={FilePlus} onClick={() => onNewFile(targetFolder)} />
          <TipButton label={MSG.newIn('folder', targetText)} icon={FolderPlus} onClick={() => onNewFolder(targetFolder)} />
          <TipButton label="Open from disk" icon={Upload} onClick={(event) => setDiskAnchor(event.currentTarget)} />
          <TipButton label="Download project as .zip" icon={Download} onClick={() => actions.downloadFolder('')} />
          <TipButton label={MSG.legendTitle} icon={CircleHelp} onClick={() => setLegendOpen(true)} />
        </div>
      </div>

      <div
        className="tree-box"
        onContextMenu={(event) => {
          event.preventDefault()
          setMenu({ position: { top: event.clientY, left: event.clientX }, path: null })
        }}
        onClick={(event) => event.target === event.currentTarget && actions.select('')}
      >
        <button
          type="button"
          className={`row root${selectedPath === '' ? ' selected' : ''}`}
          aria-pressed={selectedPath === ''}
          title={MSG.rootRowTip}
          onClick={() => actions.select('')}
        >
          <Folder className="fold" size={16} strokeWidth={1.8} />
          <span className="name">{MSG.workspaceRoot}</span>
        </button>
        {showEmpty ? (
          <div className="tree-empty">
            <p>{MSG.emptyTree}</p>
            <Button size="small" variant="outlined" color="secondary" onClick={() => onNewFile('')}>
              Create file
            </Button>
          </div>
        ) : (
          <ul className="tree" role="tree" aria-label="Workspace files" ref={treeRef} onKeyDown={onKeyDown}>
            {ctx.createRowFor('', 0)}
            {sortedChildren(tree).map((node) => (
              <TreeNode key={node.id} node={node} depth={0} ctx={ctx} />
            ))}
          </ul>
        )}
      </div>

      <TreeMenu
        position={menu?.position ?? null}
        target={menuTarget}
        onClose={() => setMenu(null)}
        onNewFile={() => onNewFile(menuParent)}
        onNewFolder={() => onNewFolder(menuParent)}
        onRename={() => onRename(menuTarget)}
        onDownload={() => (menuTarget === null ? actions.downloadFolder('') : download(menuTarget))}
        onDelete={() => onDelete(menuTarget)}
      />

      <DiskOpenControls
        anchorEl={diskAnchor}
        onClose={() => setDiskAnchor(null)}
        targetFolder={targetFolder}
        onOpenDisk={onOpenDisk}
      />
      {legendOpen && <LegendDialog onClose={() => setLegendOpen(false)} />}
    </aside>
  )
}
