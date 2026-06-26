import type {InterestStatus} from '../api/types'

const SEEKER_LABELS: Record<InterestStatus, string | null> = {
  NONE: null,
  RESPONDED: 'Вы откликнулись',
  INCOMING: 'Работодатель заинтересован',
  MUTUAL: 'Взаимный интерес!',
}

const EMPLOYER_LABELS: Record<InterestStatus, string | null> = {
  NONE: null,
  RESPONDED: 'Вы откликнулись',
  INCOMING: 'Кандидат откликнулся',
  MUTUAL: 'Взаимный интерес!',
}

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
  const resolved = status ?? 'NONE'
  if (resolved === 'NONE') return null

  const label = perspective === 'seeker' ? SEEKER_LABELS[resolved] : EMPLOYER_LABELS[resolved]
  if (label == null) return null

  return (
    <span className={`rounded-full px-3 py-1 text-xs font-semibold ${STATUS_STYLES[resolved]}`}>
      {resolved === 'MUTUAL' ? '💕 ' : ''}
      {label}
    </span>
  )
}
