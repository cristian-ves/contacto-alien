import { useMemo } from 'react'
import { ListingToolbar } from './listing/ListingToolbar'
import { ListingView } from './listing/ListingView'
import { copyText, saveText } from './listing/exportText'
import { tokenizeQuad } from './listing/syntax'
import { MSG } from '../messages'

// The Quadruples listing. Picking a line tells the parent, which marks the matching instructions in the assembly.
export function QuadsPane({ quads, fileName, selected, onPick, notify }) {
  const rows = useMemo(() => quads.map((text, index) => ({ n: index + 1, index, text })), [quads])
  const highlight = selected === null ? null : { from: selected, to: selected + 1 }
  const empty = quads.length === 0

  return (
    <section className="lpane" aria-label="Quadruples">
      <ListingToolbar
        title="Quadruples"
        detail={empty ? null : MSG.quadCount(quads.length)}
        copyTip="Copy the quadruples"
        downloadTip="Download the quadruples as text"
        disabled={empty}
        onCopy={() => copyText(quads.join('\n'), 'quadruples', notify)}
        onDownload={() => saveText(fileName, quads.join('\n') + '\n', notify)}
      />
      {empty ? (
        <p className="panel-empty" id="panel-quads" role="tabpanel">
          {MSG.emptyQuads}
        </p>
      ) : (
        <ListingView
          id="panel-quads"
          label="Quadruples"
          rows={rows}
          tokenize={tokenizeQuad}
          highlight={highlight}
          onPick={(index) => onPick(index === selected ? null : index)}
        />
      )}
    </section>
  )
}
