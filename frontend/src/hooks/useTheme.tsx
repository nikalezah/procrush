import {createContext, type ReactNode, useCallback, useContext, useEffect, useMemo, useState} from 'react'
import {applyTheme, resolveTheme} from '../theme/applyTheme'
import {getStoredTheme, setStoredTheme, type ThemePreference,} from '../theme/themeStorage'

interface ThemeContextValue {
  theme: ThemePreference
  resolvedTheme: 'light' | 'dark'
  setTheme: (theme: ThemePreference) => void
  cycleTheme: () => void
}

const ThemeContext = createContext<ThemeContextValue | null>(null)

const THEME_CYCLE: ThemePreference[] = ['system', 'light', 'dark']

export function ThemeProvider({children}: {children: ReactNode}) {
  const [theme, setThemeState] = useState<ThemePreference>(() => getStoredTheme())
  const [resolvedTheme, setResolvedTheme] = useState<'light' | 'dark'>(() =>
    resolveTheme(getStoredTheme()),
  )

  const setTheme = useCallback((next: ThemePreference) => {
    setStoredTheme(next)
    setThemeState(next)
    setResolvedTheme(applyTheme(next))
  }, [])

  const cycleTheme = useCallback(() => {
    const currentIndex = THEME_CYCLE.indexOf(theme)
    const next = THEME_CYCLE[(currentIndex + 1) % THEME_CYCLE.length]
    setTheme(next)
  }, [setTheme, theme])

  useEffect(() => {
    setResolvedTheme(applyTheme(theme))
  }, [theme])

  useEffect(() => {
    if (theme !== 'system') return

    const media = window.matchMedia('(prefers-color-scheme: dark)')
    const onChange = () => setResolvedTheme(applyTheme('system'))
    media.addEventListener('change', onChange)
    return () => media.removeEventListener('change', onChange)
  }, [theme])

  const value = useMemo(
    () => ({theme, resolvedTheme, setTheme, cycleTheme}),
    [theme, resolvedTheme, setTheme, cycleTheme],
  )

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>
}

export function useTheme(): ThemeContextValue {
  const context = useContext(ThemeContext)
  if (context == null) {
    throw new Error('useTheme must be used within ThemeProvider')
  }
  return context
}
