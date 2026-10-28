import { useEffect, useMemo, useRef, useState } from 'react'
import { buildLineModel, offsetOf, positionOf } from './lineModel'
import { classifyTokens } from './tokenCategories'
import { handleEditorKey } from './editing'

const LINE_HEIGHT = 23

// The editor only displays what the backend classified: a transparent textarea sits on top of a layer of colored
// lines. Colors come from /api/tokenize; nothing here recognises any language.
export function CodeEditor({ path, value, tokens, errors, closeBrackets, reveal, onChange, onCursor }) {
  const scrollRef = useRef(null)
  const areaRef = useRef(null)
  const handledReveal = useRef(null)
  const [currentLine, setCurrentLine] = useState(1)

  const classified = useMemo(() => classifyTokens(tokens), [tokens])
  const lines = useMemo(() => buildLineModel(value, classified, errors), [value, classified, errors])

  function syncCursor(textarea) {
    const position = positionOf(textarea.value, textarea.selectionStart)
    setCurrentLine(position.line)
    onCursor(position)
  }

  useEffect(() => {
    if (!reveal || reveal.path !== path || handledReveal.current === reveal.nonce) return
    handledReveal.current = reveal.nonce
    const textarea = areaRef.current
    const offset = offsetOf(value, reveal.line, reveal.column)
    textarea.focus()
    textarea.setSelectionRange(offset, offset)
    const scroller = scrollRef.current
    scroller.scrollTop = Math.max(0, (reveal.line - 1) * LINE_HEIGHT - scroller.clientHeight / 3)
    syncCursor(textarea)
    // Runs once per reveal request; value/onCursor are read at that moment on purpose.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [reveal, path])

  useEffect(() => {
    areaRef.current?.focus({ preventScroll: true })
    syncCursor(areaRef.current)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  return (
    <div className="editor" ref={scrollRef} role="tabpanel" id="editor-panel" aria-label={path}>
      <div className="code" onMouseDown={(event) => event.target !== areaRef.current && areaRef.current?.focus()}>
        {lines.map((line) => (
          <div
            key={line.number}
            className={`l${line.number === currentLine ? ' cur' : ''}${line.hasError ? ' err' : ''}`}
          >
            <span className="n">{line.number}</span>
            <span className="tx">
              {line.segments.map((segment, index) => (
                <span key={index} className={`tok-${segment.cat}${segment.squiggle ? ' tok-sq' : ''}`}>
                  {segment.text}
                </span>
              ))}
            </span>
          </div>
        ))}
        <textarea
          ref={areaRef}
          value={value}
          spellCheck={false}
          autoCapitalize="off"
          autoComplete="off"
          autoCorrect="off"
          wrap="off"
          aria-label="Code editor"
          onChange={(event) => {
            onChange(event.target.value)
            syncCursor(event.target)
          }}
          onSelect={(event) => syncCursor(event.target)}
          onKeyDown={(event) => handleEditorKey(event, { closeBrackets })}
        />
      </div>
    </div>
  )
}
