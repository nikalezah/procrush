async function parseError(response: Response, fallback: string): Promise<never> {
  let message = fallback
  try {
    const body = (await response.json()) as { message?: string }
    if (body.message) message = body.message
  } catch {
    // ignore
  }
  throw new Error(message)
}

async function apiFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, { credentials: 'include', ...init })
  if (!response.ok) {
    await parseError(response, `Ошибка запроса: ${response.status}`)
  }
  if (response.status === 204) return undefined as T
  return (await response.json()) as T
}

export { apiFetch, parseError }
