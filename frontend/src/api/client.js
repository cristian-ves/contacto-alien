import { MSG } from '../messages'

// The backend listens on 8080 by default; VITE_API_BASE_URL overrides it (for example in a local .env file).
const DEFAULT_BASE_URL = 'http://localhost:8080'
const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? DEFAULT_BASE_URL

// kind 'network' means the backend could not be reached at all; 'http' means it answered with an error status.
export class ApiError extends Error {
  constructor(message, kind, status) {
    super(message)
    this.kind = kind
    this.status = status
  }
}

export function apiUrl(path) {
  return `${BASE_URL}${path}`
}

export function isAbort(error) {
  return error?.name === 'AbortError'
}

async function post(path, body, signal) {
  let response
  try {
    response = await fetch(apiUrl(path), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
      signal,
    })
  } catch (error) {
    if (isAbort(error)) throw error
    throw new ApiError(MSG.backendDown, 'network')
  }

  if (!response.ok) throw await readApiError(response)

  return response.status === 204 ? null : response.json()
}

// Builds the error for a non-2xx answer. The run endpoints send a readable message in errors[0], which is used as is.
export async function readApiError(response) {
  let detail = ''
  let readable = ''
  try {
    const errorData = await response.json()
    detail = errorData?.message || errorData?.error || ''
    readable = errorData?.errors?.[0]?.message || ''
  } catch {
    // Non-JSON error body: the status alone is reported.
  }
  if (readable) return new ApiError(readable, 'http', response.status)
  return new ApiError(
    detail ? `Request failed with status ${response.status}: ${detail}` : `Request failed with status ${response.status}`,
    'http',
    response.status,
  )
}

export function tokenize({ code, language }, signal) {
  return post('/api/tokenize', { code, language }, signal)
}

export function validate({ code, language, imports, path }, signal) {
  const payload = { code, language }
  if (imports) payload.imports = imports
  if (path) payload.path = path
  return post('/api/validate', payload, signal)
}

export function compile({ code, language, imports, input }, signal) {
  const payload = { code, language, input: input ?? '' }
  if (imports) payload.imports = imports
  return post('/api/compile', payload, signal)
}

// Interactive execution: the program starts here and is then followed through runStream.js.
export function startRun({ code, language, imports, input }, signal) {
  const payload = { code, language }
  if (imports) payload.imports = imports
  if (input) payload.input = input
  return post('/api/run', payload, signal)
}

export function sendRunInput(sessionId, { text, eof }) {
  return post(`/api/run/${sessionId}/input`, { text: text ?? '', eof: Boolean(eof) })
}

export function stopRun(sessionId) {
  return post(`/api/run/${sessionId}/stop`, undefined)
}

// Used when the page is closing, where a normal request may be cancelled before it is sent.
export function stopRunOnUnload(sessionId) {
  navigator.sendBeacon?.(apiUrl(`/api/run/${sessionId}/stop`))
}
