import { useMemo, useState } from 'react'
import Autocomplete from '@mui/material/Autocomplete'
import FormControlLabel from '@mui/material/FormControlLabel'
import Checkbox from '@mui/material/Checkbox'
import TextField from '@mui/material/TextField'
import { ListingToolbar } from './listing/ListingToolbar'
import { ListingView } from './listing/ListingView'
import { buildIndex, visibleRows } from './listing/asmModel'
import { copyText, saveText } from './listing/exportText'
import { tokenizeAsm } from './listing/syntax'
import { MSG } from '../messages'

// The Assembly listing: colored, with a jump box for labels and functions and a switch that hides the fixed runtime.
// highlight is the [from, to) range of the quadruple the user picked, if any.
export function AsmPane({ lines, assembly, fileName, highlight, runtime, notify }) {
  const [programOnly, setProgramOnly] = useState(false)
  const [jump, setJump] = useState(null)
  const empty = lines.length === 0
  const index = useMemo(() => buildIndex(lines), [lines])
  const hideRuntime = programOnly && runtime !== null
  const rows = useMemo(() => visibleRows(lines, runtime, hideRuntime), [lines, runtime, hideRuntime])
  const shownText = hideRuntime ? rows.filter((row) => row.text !== undefined).map((row) => row.text).join('\n') : assembly

  return (
    <section className="lpane" aria-label="Assembly">
      <ListingToolbar
        title="Assembly"
        detail={empty ? null : MSG.asmCount(lines.length)}
        copyTip={hideRuntime ? MSG.copyProgramAsm : MSG.copyAsm}
        downloadTip={MSG.downloadAsmFull}
        disabled={empty}
        onCopy={() => copyText(shownText, 'assembly', notify)}
        onDownload={() => saveText(fileName, assembly, notify)}
      >
        {!empty && (
          <>
            <Autocomplete
              size="small"
              className="asm-jump"
              options={index}
              groupBy={(entry) => entry.group}
              getOptionLabel={(entry) => entry.name}
              isOptionEqualToValue={(a, b) => a.line === b.line}
              value={null}
              autoHighlight
              blurOnSelect
              onChange={(_, entry) => entry && setJump({ index: entry.line, nonce: Date.now() })}
              renderInput={(params) => (
                <TextField
                  {...params}
                  placeholder={MSG.asmJumpPlaceholder}
                  slotProps={{
                    ...params.slotProps,
                    htmlInput: { ...params.slotProps?.htmlInput, 'aria-label': MSG.asmJumpLabel },
                  }}
                />
              )}
            />
            <FormControlLabel
              className="asm-switch"
              label={MSG.programOnly}
              title={runtime ? MSG.programOnlyTip : MSG.programOnlyMissing}
              control={
                <Checkbox
                  size="small"
                  checked={programOnly && runtime !== null}
                  disabled={runtime === null}
                  onChange={(event) => setProgramOnly(event.target.checked)}
                />
              }
            />
          </>
        )}
      </ListingToolbar>
      {empty ? (
        <p className="panel-empty" id="panel-asm" role="tabpanel">
          {MSG.emptyAsm}
        </p>
      ) : (
        <ListingView
          id="panel-asm"
          label="Assembly"
          rows={rows}
          tokenize={tokenizeAsm}
          highlight={highlight}
          jump={jump}
        />
      )}
    </section>
  )
}
