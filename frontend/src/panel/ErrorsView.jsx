import { useState } from 'react'
import { Check } from 'lucide-react'
import { PhaseChip } from '../components/PhaseChip'
import { basename } from '../workspace/paths'
import { MSG } from '../messages'

export function ErrorsView({ errors, status, failureMessage, onJump }) {
  const [selected, setSelected] = useState(null)

  if (status === 'failed') {
    return (
      <p className="panel-empty bad" id="panel-errors" role="tabpanel">
        {failureMessage}
      </p>
    )
  }

  if (errors.length === 0) {
    return (
      <p className="panel-empty ok" id="panel-errors" role="tabpanel">
        <Check size={16} strokeWidth={2} />
        {status === 'checking' ? 'Checking' : MSG.noErrors}
      </p>
    )
  }

  return (
    <div id="panel-errors" role="tabpanel">
      <ul className="errs" role="listbox" aria-label="Errors">
        {errors.map((error) => (
          <li key={error.id}>
            <button
              type="button"
              className="er"
              role="option"
              aria-selected={selected === error.id}
              onClick={() => {
                setSelected(error.id)
                onJump(error)
              }}
            >
              <span>
                <PhaseChip phase={error.phase} />
              </span>
              <span className="m">{error.message}</span>
              <span className="loc">
                {basename(error.path)}, line {error.line}, column {error.column}
              </span>
            </button>
          </li>
        ))}
      </ul>
    </div>
  )
}
