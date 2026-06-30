const THEME_KEY = 'procrush.theme'

export type ThemePreference = 'light' | 'dark' | 'system'

export function getStoredTheme(): ThemePreference {
  const value = localStorage.getItem(THEME_KEY)
  if (value === 'light' || value === 'dark' || value === 'system') return value
  return 'system'
}

export function setStoredTheme(theme: ThemePreference): void {
  localStorage.setItem(THEME_KEY, theme)
}
