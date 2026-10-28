import { X } from 'lucide-react'
import { FileBadge } from '../components/FileBadge'
import { StateDot } from '../components/StateDot'
import { useTabKeys } from '../hooks/useTabKeys'
import { basename } from '../workspace/paths'
import { EditorOptions } from './EditorOptions'

export function EditorTabs({ openPaths, activePath, dirty, errored, onDisk, options, onSelect, onClose, onOption }) {
  const onKeyDown = useTabKeys(openPaths, onSelect)

  return (
    <div className="tabs-row">
      <div className="tabs" role="tablist" aria-label="Open files">
        {openPaths.map((path) => {
          const selected = path === activePath
          return (
            <div
              key={path}
              className="tab"
              role="tab"
              data-tab-id={path}
              aria-selected={selected}
              aria-controls="editor-panel"
              tabIndex={selected ? 0 : -1}
              title={path}
              onClick={() => onSelect(path)}
              onKeyDown={(event) => {
                onKeyDown(event)
                if (event.key === 'Delete') onClose(path)
              }}
              onAuxClick={(event) => event.button === 1 && onClose(path)}
            >
              <FileBadge path={path} />
              <span className="tab-name">{basename(path)}</span>
              {dirty.has(path) && <StateDot kind="mod" />}
              {errored.has(path) && <StateDot kind="bad" />}
              {onDisk.has(path) && <StateDot kind="disk" />}
              <button
                type="button"
                className="x"
                tabIndex={-1}
                aria-label={`Close ${basename(path)}`}
                onClick={(event) => {
                  event.stopPropagation()
                  onClose(path)
                }}
              >
                <X size={11} strokeWidth={2.2} />
              </button>
            </div>
          )
        })}
      </div>
      <EditorOptions options={options} onChange={onOption} />
    </div>
  )
}
