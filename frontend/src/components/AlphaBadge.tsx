import {useTranslation} from 'react-i18next'

export function AlphaBadge() {
  const {t} = useTranslation()
  const tooltip = t('alphaBadge.tooltip')

  return (
    <span
      className="rounded-full bg-surface-muted px-2 py-0.5 text-xs font-semibold uppercase tracking-wide text-brand-700 dark:text-brand-300"
      title={tooltip}
      aria-label={tooltip}
    >
      Alpha
    </span>
  )
}
