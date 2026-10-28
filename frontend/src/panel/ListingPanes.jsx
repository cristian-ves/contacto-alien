import { useMemo, useState } from 'react'
import { AsmPane } from './AsmPane'
import { QuadsPane } from './QuadsPane'
import { findRuntimeRange, linkQuadruples } from './listing/asmModel'

// Quadruples and assembly together: the picked quadruple lives here so the assembly can mark its instructions.
// view is 'quads', 'asm' or 'split' (both side by side).
export function ListingPanes({ view, compileState, notify }) {
  const { quads, assembly, asmName } = compileState
  // A new run brings new quadruples, so a pick made on the previous ones no longer applies.
  const [pick, setPick] = useState({ quads, index: null })
  const picked = pick.quads === quads ? pick.index : null
  const lines = useMemo(() => (assembly ? assembly.replace(/\n$/, '').split('\n') : []), [assembly])
  const runtime = useMemo(() => findRuntimeRange(lines), [lines])
  const links = useMemo(() => linkQuadruples(quads, lines, runtime ? runtime.from : lines.length), [quads, lines, runtime])
  const highlight = picked === null ? null : (links.get(picked) ?? null)
  const baseName = asmName.replace(/\.s$/, '')

  return (
    <div className={`lpanes ${view}`}>
      {view !== 'asm' && (
        <QuadsPane
          quads={quads}
          fileName={`${baseName}.quadruples.txt`}
          selected={picked}
          onPick={(index) => setPick({ quads, index })}
          notify={notify}
        />
      )}
      {view !== 'quads' && (
        <AsmPane
          lines={lines}
          assembly={assembly}
          fileName={asmName}
          highlight={highlight}
          runtime={runtime}
          notify={notify}
        />
      )}
    </div>
  )
}
