import { useCallback, useEffect, useReducer, useRef, useState } from 'react'
import { isAbort, sendRunInput, startRun, stopRun, stopRunOnUnload, ApiError } from '../api/client'
import { followRun } from '../api/runStream'
import { useBackend } from '../api/useBackend'
import { buildImportContext, countImports } from '../api/importsMap'
import { normalizeErrors } from '../api/diagnostics'
import { basename } from '../workspace/paths'
import { formatQuadruples } from '../panel/formatQuadruples'
import { consoleReducer, emptyConsole } from '../panel/consoleModel'
import { MSG } from '../messages'

const idle = { status: 'idle', quads: [], assembly: '', asmName: '', inputOpen: false }

// Runs the active .pig file as a live session: /api/run compiles and starts it, the stream brings its output as it
// is written and typed lines go back through /api/run/{id}/input. status is 'starting' while compiling, 'running'
// while the program is alive (inputOpen says whether it can still be given input), then 'done', 'rejected' or 'failed'.
// onFinish(kind) tells the shell which bottom tab to show: 'output' after a run, 'errors' when it was rejected.
export function useCompile({ tree, getContent, dispatch, onFinish, onFailure }) {
  const { report } = useBackend()
  const [session, setSession] = useState(idle)
  const [consoleState, consoleDispatch] = useReducer(consoleReducer, emptyConsole)
  const [input, setInput] = useState('')
  // The session being followed: its id (null until /api/run answers), the controller that cancels its requests and
  // a token that lets late events of a replaced run be ignored.
  const live = useRef(null)
  const tokenRef = useRef(0)
  const runRef = useRef(null)

  const say = useCallback((text, tone) => consoleDispatch({ type: 'system', text, tone }), [])

  // Stops whatever is running without waiting for the answer; used before a new run and when leaving the page.
  const release = useCallback(() => {
    const current = live.current
    live.current = null
    tokenRef.current += 1
    if (!current) return
    current.controller.abort()
    if (current.id) stopRun(current.id).catch(() => {})
  }, [])

  useEffect(() => {
    const onPageHide = () => live.current?.id && stopRunOnUnload(live.current.id)
    window.addEventListener('pagehide', onPageHide)
    return () => {
      window.removeEventListener('pagehide', onPageHide)
      release()
    }
  }, [release])

  const handleEvent = useCallback(
    ({ event, data }) => {
      if (event === 'stdout' || event === 'stderr') {
        consoleDispatch({ type: 'write', stream: event === 'stdout' ? 'out' : 'err', text: data.text })
      } else if (event === 'system') {
        say(data.text, 'system')
      } else if (event === 'exit') {
        if (data.note) say(data.note, 'err')
        say(MSG.finished(data.exitCode, data.durationMs), data.exitCode === 0 ? 'done' : 'err')
        live.current = null
        setSession((previous) => ({ ...previous, status: 'done', inputOpen: false }))
      }
    },
    [say],
  )

  // Something went wrong after the program had started: the console says what, and the program is stopped if it still runs.
  const failRun = useCallback(
    (error) => {
      const lost = error instanceof ApiError && error.kind === 'network'
      if (lost) report(false)
      const message = lost ? MSG.backendLostDuringRun : error.message
      const current = live.current
      live.current = null
      if (current?.id && !lost) stopRun(current.id).catch(() => {})
      say(message, 'error')
      setSession((previous) => ({ ...previous, status: 'failed', inputOpen: false }))
      onFailure?.(message, null)
    },
    [report, say, onFailure],
  )

  const follow = useCallback(
    async (id, controller, token) => {
      try {
        await followRun(id, { signal: controller.signal, onEvent: (event) => token === tokenRef.current && handleEvent(event) })
      } catch (error) {
        if (!isAbort(error) && token === tokenRef.current) failRun(error)
      }
    },
    [handleEvent, failRun],
  )

  const run = useCallback(
    async (path) => {
      if (live.current && live.current.id === null) return
      release()
      const token = tokenRef.current
      const controller = new AbortController()
      live.current = { id: null, controller }

      const { imports, base } = buildImportContext(tree, path, getContent)
      const name = basename(path)
      consoleDispatch({ type: 'reset' })
      say(MSG.compiling(name, countImports(imports) + 1), 'system')
      setSession({ ...idle, status: 'starting' })
      onFinish?.('output')

      try {
        const response = await startRun({ code: getContent(path), language: 'pig', imports, input }, controller.signal)
        report(true)

        if (!response.valid) {
          const errors = normalizeErrors(response.errors, path, tree, base)
          live.current = null
          dispatch({ type: 'result', path, result: { status: 'done', errors } })
          say(MSG.compileStopped(errors.length), 'error')
          setSession({ ...idle, status: 'rejected' })
          onFinish?.('errors')
          return
        }

        const quads = formatQuadruples(response.quadruples)
        const assembly = response.assembly ?? ''
        live.current = { id: response.sessionId, controller }
        dispatch({ type: 'result', path, result: { status: 'done', errors: [] } })
        say(MSG.compiled(quads.length, assembly ? assembly.split('\n').length : 0), 'system')
        if (input) say(MSG.preloadedSent(input.replace(/\n$/, '').split('\n').length), 'system')
        setSession({ status: 'running', quads, assembly, asmName: `${name.replace(/\.pig$/, '')}.s`, inputOpen: true })
        follow(response.sessionId, controller, token)
      } catch (error) {
        if (isAbort(error) || token !== tokenRef.current) return
        live.current = null
        if (error instanceof ApiError && error.kind === 'network') report(false)
        say(error.message, 'error')
        setSession({ ...idle, status: 'failed' })
        onFailure?.(error.message, () => runRef.current(path))
      }
    },
    [tree, getContent, input, report, dispatch, onFinish, onFailure, release, say, follow],
  )

  useEffect(() => {
    runRef.current = run
  }, [run])

  const stop = useCallback(() => {
    const current = live.current
    if (!current) return
    if (current.id === null) {
      release()
      say(MSG.runStopped, 'system')
      setSession(idle)
      return
    }
    // The exit event that follows closes the console with the exit code.
    stopRun(current.id).catch(failRun)
  }, [release, say, failRun])

  // The typed line is shown at once (echo) and sent with its line break; a refusal is reported next to it.
  const sendLine = useCallback(
    async (text) => {
      const current = live.current
      if (!current?.id) return
      consoleDispatch({ type: 'echo', text })
      try {
        await sendRunInput(current.id, { text: `${text}\n` })
      } catch (error) {
        if (error instanceof ApiError && error.kind === 'network') report(false)
        const message = MSG.inputFailed(error.message)
        say(message, 'error')
        onFailure?.(message, null)
      }
    },
    [report, say, onFailure],
  )

  const sendEof = useCallback(async () => {
    const current = live.current
    if (!current?.id) return
    setSession((previous) => ({ ...previous, inputOpen: false }))
    say(MSG.inputClosed, 'system')
    try {
      await sendRunInput(current.id, { eof: true })
    } catch (error) {
      const message = MSG.inputFailed(error.message)
      say(message, 'error')
      onFailure?.(message, null)
    }
  }, [say, onFailure])

  return { compileState: { ...session, console: consoleState }, input, setInput, run, stop, sendLine, sendEof }
}
