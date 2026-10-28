import { memo, useEffect, useMemo, useRef } from 'react'

function Tokens({ tokens }) {
  return tokens.map((token, index) => (
    <span key={index} className={`lx-${token.kind}`}>
      {token.text}
    </span>
  ))
}

const Row = memo(function Row({ row, tokenize, selected, onPick }) {
  const tokens = useMemo(() => tokenize(row.text ?? ''), [row.text, tokenize])
  if (row.gap !== undefined) {
    return (
      <div className="l gap-row" data-index={row.index}>
        <span className="n" />
        <span className="tx">{`... ${row.gap} lines of runtime hidden`}</span>
      </div>
    )
  }
  return (
    <div
      className={`l${selected ? ' sel' : ''}${onPick ? ' pick' : ''}`}
      data-index={row.index}
      onClick={onPick ? () => onPick(row.index) : undefined}
    >
      <span className="n">{row.n}</span>
      <span className="tx">
        <Tokens tokens={tokens} />
      </span>
    </div>
  )
})

// Read-only numbered listing with our own coloring. rows come from the caller ({ n, index, text } or a gap marker);
// highlight is a [from, to) range of line indexes to mark; jump = { index, nonce } scrolls a line into view.
export function ListingView({ id, label, rows, tokenize, highlight, jump, onPick }) {
  const box = useRef(null)

  useEffect(() => {
    if (!jump) return
    const target = box.current?.querySelector(`[data-index="${jump.index}"]`)
    target?.scrollIntoView({ block: 'center' })
  }, [jump])

  useEffect(() => {
    if (!highlight) return
    box.current?.querySelector(`[data-index="${highlight.from}"]`)?.scrollIntoView({ block: 'nearest' })
  }, [highlight])

  return (
    <div className="lscroll" ref={box} id={id} role="region" aria-label={label} tabIndex={0}>
      <div className="code listing">
        {rows.map((row) => (
          <Row
            key={row.index}
            row={row}
            tokenize={tokenize}
            selected={Boolean(highlight) && row.index >= highlight.from && row.index < highlight.to}
            onPick={onPick}
          />
        ))}
      </div>
    </div>
  )
}
