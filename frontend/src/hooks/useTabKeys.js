import { useCallback } from 'react'

// Arrow/Home/End navigation for a row of role="tab" elements. ids is the ordered list of tab ids; select(id)
// activates one and the matching element (data-tab-id) receives focus.
export function useTabKeys(ids, select) {
  return useCallback(
    (event) => {
      const current = ids.indexOf(event.currentTarget.dataset.tabId)
      let next = null
      if (event.key === 'ArrowRight') next = (current + 1) % ids.length
      else if (event.key === 'ArrowLeft') next = (current - 1 + ids.length) % ids.length
      else if (event.key === 'Home') next = 0
      else if (event.key === 'End') next = ids.length - 1
      if (next === null || ids.length === 0) return
      event.preventDefault()
      select(ids[next])
      const list = event.currentTarget.parentElement
      requestAnimationFrame(() => list.querySelector(`[data-tab-id="${CSS.escape(ids[next])}"]`)?.focus())
    },
    [ids, select],
  )
}
