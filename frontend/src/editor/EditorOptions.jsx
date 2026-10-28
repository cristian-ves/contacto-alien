import { useState } from 'react'
import Checkbox from '@mui/material/Checkbox'
import FormControlLabel from '@mui/material/FormControlLabel'
import Popover from '@mui/material/Popover'
import { Settings2 } from 'lucide-react'
import { TipButton } from '../components/TipButton'
import { MSG } from '../messages'

function Option({ checked, onChange, label, hint }) {
  return (
    <FormControlLabel
      control={<Checkbox checked={checked} onChange={(event) => onChange(event.target.checked)} />}
      label={
        <span>
          {label}
          <small className="hint">{hint}</small>
        </span>
      }
    />
  )
}

export function EditorOptions({ options, onChange }) {
  const [anchor, setAnchor] = useState(null)
  return (
    <>
      <TipButton label={MSG.options} icon={Settings2} onClick={(event) => setAnchor(event.currentTarget)} />
      <Popover
        open={Boolean(anchor)}
        anchorEl={anchor}
        onClose={() => setAnchor(null)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
        transformOrigin={{ vertical: 'top', horizontal: 'right' }}
      >
        <div className="options-card">
          <Option
            checked={options.validateOnType}
            onChange={(value) => onChange('validateOnType', value)}
            label={MSG.validateWhileTyping}
            hint={MSG.validateWhileTypingHint}
          />
          <Option
            checked={options.closeBrackets}
            onChange={(value) => onChange('closeBrackets', value)}
            label={MSG.closeBrackets}
            hint={MSG.closeBracketsHint}
          />
        </div>
      </Popover>
    </>
  )
}
