const LOCALE_KEY = 'procrush.locale'

export type AppLocale = 'ru' | 'en'

export function getStoredLocale(): AppLocale | null {
  const value = localStorage.getItem(LOCALE_KEY)
  if (value === 'ru' || value === 'en') return value
  return null
}

export function setStoredLocale(locale: AppLocale): void {
  localStorage.setItem(LOCALE_KEY, locale)
}

export function clearStoredLocale(): void {
  localStorage.removeItem(LOCALE_KEY)
}
