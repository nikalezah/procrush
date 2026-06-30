import {useTranslation} from 'react-i18next'
import type {InterestStatus} from '../api/types'

const STATUS_STYLES: Record<InterestStatus, string> = {
  NONE: '',
  RESPONDED:
    'bg-sky-700 text-white dark:bg-sky-600 dark:text-white',
  INCOMING:
    'bg-brand-700 text-white dark:bg-brand-600 dark:text-white',
  MUTUAL:
    'bg-accent-700 text-white shadow-sm dark:bg-accent-600 dark:text-white',
}

interface InterestStatusBadgeProps {
  status: InterestStatus | undefined
  perspective: 'seeker' | 'employer'
}

export function InterestStatusBadge({status, perspective}: InterestStatusBadgeProps) {
  const {t} = useTranslation()
  const resolved = status ?? 'NONE'
  if (resolved === 'NONE') return null

  const labelKey = `components.interestStatus.${perspective}.${resolved.toLowerCase()}` as const
  const label = t(labelKey)

  return (
    <span className={`rounded-full px-3 py-1 text-xs font-semibold ${STATUS_STYLES[resolved]}`}>
      {label}
    </span>
  )
}
