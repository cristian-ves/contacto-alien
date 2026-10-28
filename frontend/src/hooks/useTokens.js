import { useEffect, useState } from 'react'
import { tokenize, isAbort, ApiError } from '../api/client'
import { useBackend } from '../api/useBackend'

const DEBOUNCE_MS = 350

// Asks the backend to classify the code after a pause; the previous request is aborted when newer text arrives.
// The answer is stored together with the path it belongs to so tokens never get painted over another file.
export function useTokens(path, code, language) {
  const [state, setState] = useState({ path: null, tokens: [], failed: false })
  const { report } = useBackend()

  useEffect(() => {
    if (!path || !language) return undefined
    const controller = new AbortController()
    const delay = state.path === path ? DEBOUNCE_MS : 0
    const timer = setTimeout(async () => {
      try {
        const response = await tokenize({ code, language }, controller.signal)
        report(true)
        setState({ path, tokens: Array.isArray(response?.tokens) ? response.tokens : [], failed: false })
      } catch (error) {
        if (isAbort(error)) return
        if (error instanceof ApiError && error.kind === 'network') report(false)
        setState((prev) => ({ path, tokens: prev.path === path ? prev.tokens : [], failed: true }))
      }
    }, delay)

    return () => {
      clearTimeout(timer)
      controller.abort()
    }
    // state.path only decides the delay; including it would re-run the effect after every response.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [path, code, language, report])

  return state.path === path ? state : { path, tokens: [], failed: false }
}
