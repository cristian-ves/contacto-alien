import { ConsoleBar } from './ConsoleBar'
import { ConsoleView } from './ConsoleView'
import { MSG } from '../messages'

// The Output tab: a console that shows what the program writes as it writes it and takes what the user types.
export function OutputView({ compileState, preloaded, onPreloaded, onSendLine, onEndInput }) {
  const { console: model, status, inputOpen } = compileState
  const running = status === 'running'
  const inputActive = running && inputOpen

  return (
    <div id="panel-output" role="tabpanel">
      <ConsoleBar
        preloaded={preloaded}
        onPreloaded={onPreloaded}
        locked={status === 'starting' || running}
        canEnd={inputActive}
        onEnd={onEndInput}
      />
      {model.lines.length === 0 && !inputActive ? (
        <p className="panel-empty">{MSG.emptyOutput}</p>
      ) : (
        <ConsoleView model={model} inputActive={inputActive} onSubmit={onSendLine} onEnd={onEndInput} />
      )}
    </div>
  )
}
