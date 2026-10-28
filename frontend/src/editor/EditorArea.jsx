import Button from '@mui/material/Button'
import { FilePlus } from 'lucide-react'
import { CodeEditor } from './CodeEditor'
import { EditorTabs } from './EditorTabs'
import { useTokens } from '../hooks/useTokens'
import { detectLanguageFromFilename } from '../api/languages'
import { MSG } from '../messages'

function EmptyEditor({ onNewFile }) {
  return (
    <div className="editor empty">
      <p>{MSG.emptyEditor}</p>
      <Button variant="outlined" color="secondary" size="small" startIcon={<FilePlus size={16} />} onClick={onNewFile}>
        Create file
      </Button>
    </div>
  )
}

export function EditorArea({ ide, errorsByPath, onNewFile, onClose }) {
  const { state, actions } = ide
  const { activePath, drafts, handles, openPaths, options, reveal } = state
  const content = activePath ? actions.getContent(activePath) : ''
  const language = activePath ? detectLanguageFromFilename(activePath) : null
  const { tokens, failed } = useTokens(activePath, content, language)

  const dirty = new Set(Object.keys(drafts))
  const errored = new Set(errorsByPath.keys())

  return (
    <div className="center-top">
      <EditorTabs
        openPaths={openPaths}
        activePath={activePath}
        dirty={dirty}
        errored={errored}
        onDisk={new Set(Object.keys(handles))}
        options={options}
        onSelect={actions.activate}
        onClose={onClose}
        onOption={actions.setOption}
      />
      {activePath ? (
        <div className="editor-wrap">
          <CodeEditor
            key={activePath}
            path={activePath}
            value={content}
            tokens={tokens}
            errors={errorsByPath.get(activePath) ?? []}
            closeBrackets={options.closeBrackets}
            reveal={reveal}
            onChange={(text) => actions.edit(activePath, text)}
            onCursor={actions.setCursor}
          />
          {failed && <div className="floating-note">{MSG.highlightUnavailable}</div>}
        </div>
      ) : (
        <EmptyEditor onNewFile={onNewFile} />
      )}
    </div>
  )
}
