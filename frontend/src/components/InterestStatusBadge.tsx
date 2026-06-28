import {useTranslation} from 'react-i18next'
import type {InterestStatus} from '../api/types'

const STATUS_STYLES: Record<InterestStatus, string> = {
  NONE: '',
  RESPONDED: 'bg-sky-100 text-sky-800',
  INCOMING: 'bg-brand-100 text-brand-800',
  MUTUAL: 'bg-emerald-100 text-emerald-800 ring-1 ring-emerald-200',
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
      {resolved === 'MUTUAL' ? '💕 ' : ''}
      {label}
    </span>
  )
}
