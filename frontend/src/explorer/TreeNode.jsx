import Collapse from '@mui/material/Collapse'
import { ChevronRight, Folder } from 'lucide-react'
import { FileBadge } from '../components/FileBadge'
import { StateDot } from '../components/StateDot'
import { sortedChildren } from './visibleRows'

export function TreeNode({ node, depth, ctx }) {
  const renaming = ctx.renameRowFor(node.path, depth)
  if (renaming) return renaming

  const isFolder = node.type === 'folder'
  const isOpen = isFolder && Boolean(ctx.expanded[node.path])
  const isActive = node.path === ctx.activePath
  const isSelected = node.path === ctx.selectedPath

  return (
    <li role="none">
      <button
        type="button"
        className={`row ${isFolder ? 'folder' : 'file'}${isSelected && isFolder ? ' selected' : ''}`}
        role="treeitem"
        data-path={node.path}
        style={{ '--d': depth }}
        aria-level={depth + 1}
        aria-expanded={isFolder ? isOpen : undefined}
        aria-current={isActive ? 'true' : undefined}
        tabIndex={node.path === ctx.tabStop ? 0 : -1}
        onClick={() => ctx.onActivate(node)}
        onContextMenu={(event) => ctx.onContext(event, node)}
      >
        {isFolder ? (
          <>
            <ChevronRight className="chev" size={12} strokeWidth={2.2} />
            <Folder className="fold" size={16} strokeWidth={1.8} />
          </>
        ) : (
          <>
            <span className="gap" />
            <FileBadge path={node.path} />
          </>
        )}
        <span className="name">{node.name}</span>
        {ctx.dirty.has(node.path) && <StateDot kind="mod" />}
        {ctx.errored.has(node.path) && <StateDot kind="bad" />}
        {ctx.onDisk.has(node.path) && <StateDot kind="disk" />}
      </button>
      {isFolder && (
        <Collapse in={isOpen} timeout={ctx.motion} unmountOnExit>
          <ul role="group">
            {ctx.createRowFor(node.path, depth + 1)}
            {sortedChildren(node).map((child) => (
              <TreeNode key={child.id} node={child} depth={depth + 1} ctx={ctx} />
            ))}
          </ul>
        </Collapse>
      )}
    </li>
  )
}
