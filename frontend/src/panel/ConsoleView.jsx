import { useState } from 'react'
import { hasOpenPrompt, sourceLabel } from './consoleModel'
import { ConsoleInput } from './ConsoleInput'
import { useStickToBottom } from './useStickToBottom'
import { MSG } from '../messages'

function Line({ line, children }) {
  return (
    <div className={`cl ${line.tone}`}>
      <span className="ts">{line.ts}</span>
      <span className="src">{sourceLabel(line.stream)}</span>
      <span className={children ? 'msg with-input' : 'msg'}>
        <span className="text">
          {line.parts.map((part, index) => (
            <span key={index} className={part.kind === 'echo' ? 'echo' : undefined}>
              {part.text}
            </span>
          ))}
        </span>
        {children}
      </span>
    </div>
  )
}

// The program's output as it arrives. While the program can still read input, the typing line sits at the end:
// right after an unfinished prompt such as "> ", or on a line of its own.
export function ConsoleView({ model, inputActive, onSubmit, onEnd }) {
  const [draft, setDraft] = useState('')
  const endRef = useStickToBottom(model.lines)
  const inline = inputActive && hasOpenPrompt(model)
  const lastIndex = model.lines.length - 1

  function submit() {
    onSubmit(draft)
    setDraft('')
  }

  const field = (placeholder) => (
    <ConsoleInput value={draft} onChange={setDraft} onSubmit={submit} onEnd={onEnd} placeholder={placeholder} />
  )

  return (
    <div className="console" role="log" aria-live="polite">
      {model.hidden > 0 && (
        <div className="cl system">
          <span className="msg">{MSG.earlierHidden(model.hidden)}</span>
        </div>
      )}
      {model.lines.map((line, index) => (
        <Line key={line.id} line={line}>
          {inline && index === lastIndex ? field(undefined) : null}
        </Line>
      ))}
      {inputActive && !inline && (
        <div className="cl in">
          <span className="ts" />
          <span className="src">{sourceLabel('in')}</span>
          <span className="msg with-input">{field(MSG.consoleInputPlaceholder)}</span>
        </div>
      )}
      <div ref={endRef} />
    </div>
  )
}
