import { useEffect, useRef } from 'react'

const NEAR_BOTTOM_PX = 48

// Keeps a growing console scrolled to its end, unless the user scrolled up to read: then it stays where it is.
// The scrolling parent is the results panel body. trigger changes whenever content is added.
export function useStickToBottom(trigger) {
  const endRef = useRef(null)
  const stuck = useRef(true)

  useEffect(() => {
    const scroller = endRef.current?.closest('.pbody')
    if (!scroller) return undefined
    const onScroll = () => {
      stuck.current = scroller.scrollHeight - scroller.scrollTop - scroller.clientHeight < NEAR_BOTTOM_PX
    }
    scroller.addEventListener('scroll', onScroll)
    return () => scroller.removeEventListener('scroll', onScroll)
  }, [])

  useEffect(() => {
    if (stuck.current) endRef.current?.scrollIntoView?.({ block: 'nearest' })
  }, [trigger])

  return endRef
}
