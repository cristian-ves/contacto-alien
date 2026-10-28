import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { tokenize } from './client'
import { BackendContext } from './backendContext'

const RECHECK_MS = 5000

// Tracks whether the backend answers. Requests made elsewhere report their outcome; while it is down a light
// tokenize call is retried until it responds, so the banner clears without a manual reload.
export function BackendStatusProvider({ children }) {
  const [status, setStatus] = useState('checking')
  const statusRef = useRef(status)

  const report = useCallback((reachable) => {
    const next = reachable ? 'up' : 'down'
    if (statusRef.current !== next) {
      statusRef.current = next
      setStatus(next)
    }
  }, [])

  const recheck = useCallback(async () => {
    try {
      await tokenize({ code: '', language: 'pig' })
      report(true)
    } catch {
      report(false)
    }
  }, [report])

  useEffect(() => {
    recheck()
  }, [recheck])

  useEffect(() => {
    if (status !== 'down') return undefined
    const timer = setInterval(recheck, RECHECK_MS)
    return () => clearInterval(timer)
  }, [status, recheck])

  const value = useMemo(() => ({ status, report, recheck }), [status, report, recheck])
  return <BackendContext.Provider value={value}>{children}</BackendContext.Provider>
}
