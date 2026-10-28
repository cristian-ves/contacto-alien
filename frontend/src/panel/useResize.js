import { useCallback, useRef, useState } from 'react'

const MIN = 120
const STEP = 24

// Vertical splitter: dragging up makes the bottom panel taller. The max keeps the editor usable.
export function useResize(initial) {
  const [height, setHeight] = useState(initial)
  const drag = useRef(null)

  const clamp = (value) => Math.max(MIN, Math.min(value, window.innerHeight - 300))

  const onPointerDown = useCallback(
    (event) => {
      drag.current = { startY: event.clientY, startHeight: height }
      event.currentTarget.setPointerCapture(event.pointerId)
    },
    [height],
  )

  const onPointerMove = useCallback((event) => {
    if (!drag.current) return
    setHeight(clamp(drag.current.startHeight - (event.clientY - drag.current.startY)))
  }, [])

  const onPointerUp = useCallback(() => {
    drag.current = null
  }, [])

  const onKeyDown = useCallback((event) => {
    if (event.key === 'ArrowUp') setHeight((value) => clamp(value + STEP))
    else if (event.key === 'ArrowDown') setHeight((value) => clamp(value - STEP))
    else return
    event.preventDefault()
  }, [])

  return { height, handlers: { onPointerDown, onPointerMove, onPointerUp, onKeyDown } }
}
