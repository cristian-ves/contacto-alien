import { useState } from 'react'
import Button from '@mui/material/Button'
import Collapse from '@mui/material/Collapse'
import Tooltip from '@mui/material/Tooltip'
import { ChevronRight } from 'lucide-react'
import { NO_AUTOFILL } from '../components/noAutofill'
import { MSG } from '../messages'

// Top of the console: the folded "preloaded input" option (lines sent when the program starts) and, while a program
// can read, the action that closes its input.
export function ConsoleBar({ preloaded, onPreloaded, locked, canEnd, onEnd }) {
  const [open, setOpen] = useState(false)
  const count = preloaded ? preloaded.replace(/\n$/, '').split('\n').length : 0

  return (
    <div className="console-bar">
      <div className="console-bar-row">
        <button
          type="button"
          className="console-fold"
          aria-expanded={open}
          aria-controls="preloaded-input"
          onClick={() => setOpen(!open)}
        >
          <ChevronRight size={14} className={open ? 'turned' : undefined} />
          {MSG.preloadedInput}
          {count > 0 && <span className="fold-count">{MSG.preloadedCount(count)}</span>}
        </button>
        <span className="sp" />
        {canEnd && (
          <Tooltip title={MSG.endInputTip} placement="top" describeChild>
            <Button size="small" color="inherit" onClick={onEnd}>
              {MSG.endInput}
            </Button>
          </Tooltip>
        )}
      </div>
      <Collapse in={open} unmountOnExit>
        <div id="preloaded-input" className="preloaded">
          <textarea
            className="inp"
            rows={3}
            value={preloaded}
            disabled={locked}
            placeholder={MSG.preloadedInputPlaceholder}
            aria-label={MSG.preloadedInput}
            onChange={(event) => onPreloaded(event.target.value)}
            {...NO_AUTOFILL}
          />
          <p className="hint">{MSG.preloadedInputHint}</p>
        </div>
      </Collapse>
    </div>
  )
}
