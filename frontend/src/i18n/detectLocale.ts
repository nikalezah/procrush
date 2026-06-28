import {type AppLocale, getStoredLocale} from './localeStorage'

function browserLocale(): AppLocale {
  const lang = navigator.language.toLowerCase()
  if (lang.startsWith('en')) return 'en'
  return 'ru'
}

export function detectLocale(): AppLocale {
  return getStoredLocale() ?? browserLocale()
}

export function applyDocumentLocale(locale: AppLocale): void {
  document.documentElement.lang = locale
}
