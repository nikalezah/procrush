import {apiFetch} from './client'
import type {AuthUserDto, CompleteRegistrationRequest, DevLoginRequest} from './types'

const jsonHeaders = { 'Content-Type': 'application/json' }

export async function fetchMe(): Promise<AuthUserDto | null> {
  const response = await fetch('/api/auth/me', { credentials: 'include' })
  if (!response.ok) return null
  const body = await response.json()
  const user = body.user ?? null
  if (user == null || user.id === '' || user.email === '') return null
  return user
}

export function devLogin(email: string): Promise<AuthUserDto> {
  const body: DevLoginRequest = { email }
  return apiFetch('/api/auth/dev/login', {
    method: 'POST',
    headers: jsonHeaders,
    body: JSON.stringify(body),
  })
}

export function logout(): Promise<void> {
  return apiFetch('/api/auth/logout', { method: 'POST' })
}

export function completeRegistration(body: CompleteRegistrationRequest): Promise<AuthUserDto> {
  return apiFetch('/api/auth/complete-registration', {
    method: 'POST',
    headers: jsonHeaders,
    body: JSON.stringify(body),
  })
}

export function deleteAccount(): Promise<void> {
  return apiFetch('/api/auth/account', { method: 'DELETE' })
}
