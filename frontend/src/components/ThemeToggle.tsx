import {useTranslation} from 'react-i18next'
import {useTheme} from '../hooks/useTheme'
import type {ThemePreference} from '../theme/themeStorage'
import {Button} from './ui/Button'

interface ThemeToggleProps {
  variant?: 'full' | 'compact'
}

const THEME_ICONS: Record<ThemePreference, string> = {
  light: '☀️',
  dark: '🌙',
  system: '💻',
}

export function ThemeToggle({variant = 'full'}: ThemeToggleProps) {
  const {t} = useTranslation()
  const {theme, setTheme, cycleTheme} = useTheme()

  if (variant === 'compact') {
    return (
      <button
        type="button"
        onClick={cycleTheme}
        title={t(`settings.appearance.${theme}`)}
        aria-label={t(`settings.appearance.${theme}`)}
        className="inline-flex h-9 w-9 items-center justify-center rounded-full border border-border text-base transition hover:bg-surface-muted"
      >
        {THEME_ICONS[theme]}
      </button>
    )
  }

  return (
    <div className="flex flex-wrap gap-2">
      {(['light', 'dark', 'system'] as const).map((option) => (
        <Button
          key={option}
          type="button"
          variant={theme === option ? 'primary' : 'secondary'}
          onClick={() => setTheme(option)}
        >
          {t(`settings.appearance.${option}`)}
        </Button>
      ))}
    </div>
  )
}
