import Button from '@mui/material/Button'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogTitle from '@mui/material/DialogTitle'
import { LANGUAGES } from '../api/languages'
import { MSG } from '../messages'

const DOTS = ['mod', 'bad', 'disk']

// What the colored dots and the letter badges next to file names mean.
export function LegendDialog({ onClose }) {
  return (
    <Dialog open onClose={onClose} aria-labelledby="legend-title">
      <DialogTitle id="legend-title">{MSG.legendTitle}</DialogTitle>
      <DialogContent>
        <ul className="legend">
          {DOTS.map((kind) => (
            <li key={kind}>
              <span className={`dot ${kind}`} aria-hidden="true" />
              <span>{MSG.dotLabels[kind]}</span>
            </li>
          ))}
          {[...LANGUAGES].reverse().map((language) => (
            <li key={language.id}>
              <span className={`ft ${language.tone}`} aria-hidden="true">
                {language.badge}
              </span>
              <span>
                {language.label} file ({language.extension})
              </span>
            </li>
          ))}
        </ul>
      </DialogContent>
      <DialogActions>
        <Button color="inherit" onClick={onClose}>
          Close
        </Button>
      </DialogActions>
    </Dialog>
  )
}
