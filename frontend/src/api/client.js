const BASE_URL = import.meta.env.VITE_API_BASE_URL

async function handleResponse(response) {
  if (!response.ok) {
    let detail = ''
    try {
      const errorData = await response.json()
      detail = errorData?.message || errorData?.error || ''
    } catch {
      // Non-JSON response body fallback
    }

    const message = detail
      ? `Request failed with status ${response.status}: ${detail}`
      : `Request failed with status ${response.status}`

    throw new Error(message)
  }

  return response.json()
}

export async function tokenize({ code, language }) {
  const response = await fetch(`${BASE_URL}/api/tokenize`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ code, language }),
  })

  return handleResponse(response)
}

export async function validate({ code, language, imports }) {
  const payload = { code, language }
  if (imports !== undefined && imports !== null) {
    payload.imports = imports
  }

  const response = await fetch(`${BASE_URL}/api/validate`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  })

  return handleResponse(response)
}
