import { Columns2 } from 'lucide-react'
import { useState } from 'react'
import { useTabKeys } from '../hooks/useTabKeys'
import { TipButton } from '../components/TipButton'
import { ErrorsView } from './ErrorsView'
import { ListingPanes } from './ListingPanes'
import { OutputView } from './OutputView'
import { MSG } from '../messages'

const TABS = ['output', 'errors', 'quads', 'asm']
const LABELS = { output: 'Output', errors: 'Errors', quads: 'Quadruples', asm: 'Assembly' }

export function BottomPanel({ tab, onTab, errors, validation, compileState, compiler, notify, onJump }) {
  const onKeyDown = useTabKeys(TABS, onTab)
  const [split, setSplit] = useState(false)
  const listing = tab === 'quads' || tab === 'asm'

  return (
    <section className="bottom" aria-label="Results">
      <div className="ptabs" role="tablist" aria-label="Results">
        {TABS.map((id) => (
          <button
            key={id}
            type="button"
            className="ptab"
            role="tab"
            data-tab-id={id}
            aria-selected={tab === id}
            aria-controls={`panel-${id}`}
            tabIndex={tab === id ? 0 : -1}
            onClick={() => onTab(id)}
            onKeyDown={onKeyDown}
          >
            {LABELS[id]}
            {id === 'errors' && errors.length > 0 && <span className="count">{errors.length}</span>}
          </button>
        ))}
        <span className="sp" />
        <TipButton
          label={split ? MSG.singleView : MSG.splitView}
          icon={Columns2}
          aria-pressed={split}
          className={split ? 'on' : undefined}
          onClick={() => setSplit(!split)}
        />
      </div>
      <div className={`pbody${listing ? ' listing-body' : ''}`} key={listing ? 'listing' : tab}>
        {tab === 'output' && (
          <OutputView
            compileState={compileState}
            preloaded={compiler.input}
            onPreloaded={compiler.setInput}
            onSendLine={compiler.sendLine}
            onEndInput={compiler.sendEof}
          />
        )}
        {tab === 'errors' && (
          <ErrorsView
            errors={errors}
            status={validation?.status}
            failureMessage={validation?.message}
            onJump={onJump}
          />
        )}
        {listing && <ListingPanes view={split ? 'split' : tab} compileState={compileState} notify={notify} />}
      </div>
    </section>
  )
}
