import { ApiError, apiUrl, isAbort, readApiError } from './client'
import { MSG } from '../messages'

const MAX_RECONNECTS = 3
const RECONNECT_DELAY_MS = 1000

// Splits a server-sent-events text stream into events: { id, event, data } with data already parsed as JSON.
// Lines that start with ':' are comments (the backend's keep-alive) and carry nothing.
export function createSseParser(onEvent) {
  let buffer = ''

  function dispatch(block) {
    const event = { id: null, event: 'message', data: null }
    const data = []
    for (const line of block.split('\n')) {
      if (line === '' || line.startsWith(':')) continue
      const colon = line.indexOf(':')
      const field = colon === -1 ? line : line.slice(0, colon)
      const value = colon === -1 ? '' : line.slice(colon + 1).replace(/^ /, '')
      if (field === 'id') event.id = value
      else if (field === 'event') event.event = value
      else if (field === 'data') data.push(value)
    }
    if (data.length === 0) return
    try {
      event.data = JSON.parse(data.join('\n'))
    } catch {
      return
    }
    onEvent(event)
  }

  return {
    push(chunk) {
      buffer += chunk.replace(/\r\n/g, '\n')
      let end = buffer.indexOf('\n\n')
      while (end !== -1) {
        dispatch(buffer.slice(0, end))
        buffer = buffer.slice(end + 2)
        end = buffer.indexOf('\n\n')
      }
    },
  }
}

function pause(ms, signal) {
  return new Promise((resolve, reject) => {
    const timer = setTimeout(resolve, ms)
    signal?.addEventListener(
      'abort',
      () => {
        clearTimeout(timer)
        reject(new DOMException('Aborted', 'AbortError'))
      },
      { once: true },
    )
  })
}

// One connection to the stream; returns when the server closes it.
async function readOnce(sessionId, lastId, signal, onEvent) {
  const headers = { Accept: 'text/event-stream' }
  if (lastId !== null) headers['Last-Event-ID'] = lastId
  let response
  try {
    response = await fetch(apiUrl(`/api/run/${sessionId}/stream`), { headers, signal })
  } catch (error) {
    if (isAbort(error)) throw error
    throw new ApiError(MSG.backendDown, 'network')
  }
  if (!response.ok) throw await readApiError(response)

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  const parser = createSseParser(onEvent)
  try {
    for (;;) {
      const { done, value } = await reader.read()
      if (done) return
      parser.push(decoder.decode(value, { stream: true }))
    }
  } catch (error) {
    if (isAbort(error)) throw error
    throw new ApiError(MSG.streamCut, 'stream')
  }
}

/**
Follows a run session until its exit event, calling onEvent for every event. If the connection drops it
reconnects a few times, resuming after the last event received so nothing is shown twice. It gives up with
an ApiError: kind 'network' when the backend cannot be reached, 'http' when the session is gone (the
message comes from the backend), 'stream' when the connection kept breaking.
*/
export async function followRun(sessionId, { signal, onEvent }) {
  let lastId = null
  let failures = 0
  let exited = false
  while (!exited) {
    try {
      await readOnce(sessionId, lastId, signal, (event) => {
        failures = 0
        if (event.id !== null) lastId = event.id
        if (event.event === 'exit') exited = true
        onEvent(event)
      })
      if (!exited) throw new ApiError(MSG.streamCut, 'stream')
    } catch (error) {
      if (isAbort(error) || error.kind === 'http') throw error
      failures += 1
      if (failures > MAX_RECONNECTS) throw error
      await pause(RECONNECT_DELAY_MS, signal)
    }
  }
}
