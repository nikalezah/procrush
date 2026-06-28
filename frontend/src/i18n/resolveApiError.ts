import i18n from './config'
import {isErrorCode} from '../../../i18n/generated/typescript/errorCodes'

export function resolveApiError(
  code: string | undefined,
  message: string,
  details?: Record<string, string>,
): string {
  if (code != null && isErrorCode(code)) {
    const translated = i18n.t(code, {ns: 'errors', ...details})
    if (translated !== code) return translated
  }
  return message
}

/** DB field may store an ErrorCode or legacy Russian text from before i18n. */
export function resolveStoredErrorCode(value: string | null | undefined, fallback: string): string {
  if (value == null || value === '') return fallback
  if (isErrorCode(value)) {
    const translated = i18n.t(value, {ns: 'errors'})
    if (translated !== value) return translated
  }
  return value
}

export function resolveError(err: unknown): string {
  if (err instanceof ApiError) {
    return resolveApiError(err.code, err.message, err.details)
  }
  if (err instanceof Error) return err.message
  return i18n.t('UNKNOWN_ERROR', {ns: 'errors'})
}

export class ApiError extends Error {
  constructor(
    public code: string,
    message: string,
    public details?: Record<string, string>,
  ) {
    super(message)
    this.name = 'ApiError'
  }
}
