import { useCallback, useEffect, useRef } from 'react'
import { validate, isAbort, ApiError } from '../api/client'
import { useBackend } from '../api/useBackend'
import { buildImportContext } from '../api/importsMap'
import { normalizeErrors } from '../api/diagnostics'
import { detectLanguageFromFilename } from '../api/languages'
import { MSG } from '../messages'

const PAUSE_MS = 600

// Validates the active file against the backend. Typing triggers it after a pause (when enabled); saving and the
// Validate button call validateNow. Only the newest request lives: starting one aborts the previous.
export function useValidation({ tree, activePath, getContent, enabled, dispatch }) {
  const { report } = useBackend()
  const controllerRef = useRef(null)
  const latest = useRef({ tree, activePath, getContent })
  useEffect(() => {
    latest.current = { tree, activePath, getContent }
  })

  const run = useCallback(async () => {
    const { tree: currentTree, activePath: path, getContent: read } = latest.current
    const language = path && detectLanguageFromFilename(path)
    if (!language) return
    controllerRef.current?.abort()
    const controller = new AbortController()
    controllerRef.current = controller

    dispatch({ type: 'result', path, result: { status: 'checking', errors: [] } })
    try {
      const { imports, path: classPath, base } = buildImportContext(currentTree, path, read)
      const response = await validate({ code: read(path), language, imports, path: classPath }, controller.signal)
      report(true)
      dispatch({
        type: 'result',
        path,
        result: { status: 'done', errors: normalizeErrors(response.errors, path, currentTree, base) },
      })
    } catch (error) {
      if (isAbort(error)) return
      if (error instanceof ApiError && error.kind === 'network') report(false)
      dispatch({ type: 'result', path, result: { status: 'failed', errors: [], message: error.message || MSG.checkFailed } })
    }
  }, [dispatch, report])

  const activeContent = activePath ? getContent(activePath) : null

  useEffect(() => {
    if (!enabled || activePath === null) return undefined
    const timer = setTimeout(run, PAUSE_MS)
    return () => clearTimeout(timer)
  }, [enabled, activePath, activeContent, tree, run])

  useEffect(() => () => controllerRef.current?.abort(), [])

  return { validateNow: run }
}
