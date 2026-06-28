import {useTranslation} from 'react-i18next'

export function AlphaBadge() {
  const {t} = useTranslation()
  const tooltip = t('alphaBadge.tooltip')

  return (
    <span
      className="rounded-full bg-brand-100 px-2 py-0.5 text-xs font-semibold uppercase tracking-wide text-brand-700"
      title={tooltip}
      aria-label={tooltip}
    >
      Alpha
    </span>
  )
}
