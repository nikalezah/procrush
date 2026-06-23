import type {AuthUserDto, CompleteRegistrationRequest, DevLoginRequest, MeResponse,} from './types'

const jsonHeaders = { 'Content-Type': 'application/json' }

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

export async function fetchMe(): Promise<AuthUserDto | null> {
  const response = await fetch('/api/auth/me', { credentials: 'include' })
  if (!response.ok) return null
  const body = (await response.json()) as MeResponse
  return body.user
}

export async function devLogin(email: string): Promise<AuthUserDto> {
  const body: DevLoginRequest = { email }
  const response = await fetch('/api/auth/dev/login', {
    method: 'POST',
    credentials: 'include',
    headers: jsonHeaders,
    body: JSON.stringify(body),
  })
  if (!response.ok) {
    await parseError(response, `Не удалось войти: ${response.status}`)
  }
  return (await response.json()) as AuthUserDto
}

export async function logout(): Promise<void> {
  await fetch('/api/auth/logout', {
    method: 'POST',
    credentials: 'include',
  })
}

export async function completeRegistration(
  body: CompleteRegistrationRequest,
): Promise<AuthUserDto> {
  const response = await fetch('/api/auth/complete-registration', {
    method: 'POST',
    credentials: 'include',
    headers: jsonHeaders,
    body: JSON.stringify(body),
  })
  if (!response.ok) {
    await parseError(response, `Не удалось завершить регистрацию: ${response.status}`)
  }
  return (await response.json()) as AuthUserDto
}

export async function deleteAccount(): Promise<void> {
  const response = await fetch('/api/auth/account', {
    method: 'DELETE',
    credentials: 'include',
  })
  if (!response.ok) {
    await parseError(response, `Не удалось удалить аккаунт: ${response.status}`)
  }
}
