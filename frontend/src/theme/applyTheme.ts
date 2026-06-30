import type {ThemePreference} from './themeStorage'

const THEME_COLOR_LIGHT = '#e11d48'
const THEME_COLOR_DARK = '#881337'

export function resolveTheme(preference: ThemePreference): 'light' | 'dark' {
  if (preference === 'light') return 'light'
  if (preference === 'dark') return 'dark'
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

export function applyTheme(preference: ThemePreference): 'light' | 'dark' {
  const resolved = resolveTheme(preference)
  const root = document.documentElement

  root.classList.toggle('dark', resolved === 'dark')

  const meta = document.querySelector('meta[name="theme-color"]')
  if (meta != null) {
    meta.setAttribute('content', resolved === 'dark' ? THEME_COLOR_DARK : THEME_COLOR_LIGHT)
  }

  return resolved
}
