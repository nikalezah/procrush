import i18n from '../i18n/config'
import {ApiError} from '../i18n/resolveApiError'

async function parseError(response: Response, fallback: string): Promise<never> {
  let code = 'INVALID_REQUEST'
  let message = fallback
  let details: Record<string, string> | undefined
  try {
    const body = (await response.json()) as {
      code?: string
      message?: string
      details?: Record<string, string>
    }
    if (body.code) code = body.code
    if (body.message) message = body.message
    details = body.details
  } catch {
    // ignore
  }
  throw new ApiError(code, message, details)
}

async function apiFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, {credentials: 'include', ...init})
  if (!response.ok) {
    await parseError(
      response,
      i18n.t('api.requestError', {status: response.status}),
    )
  }
  if (response.status === 204) return undefined as T
  return (await response.json()) as T
}

export {apiFetch}
export {ApiError} from '../i18n/resolveApiError'
