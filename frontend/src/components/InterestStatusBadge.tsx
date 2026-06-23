import type {InterestStatus} from '../api/types'

const SEEKER_LABELS: Record<InterestStatus, string | null> = {
  NONE: null,
  RESPONDED: 'Вы откликнулись',
  INCOMING: 'Работодатель заинтересован',
  MUTUAL: 'Взаимный интерес',
}

const EMPLOYER_LABELS: Record<InterestStatus, string | null> = {
  NONE: null,
  RESPONDED: 'Вы откликнулись',
  INCOMING: 'Кандидат откликнулся',
  MUTUAL: 'Взаимный интерес',
}

const STATUS_STYLES: Record<InterestStatus, string> = {
  NONE: '',
  RESPONDED: 'bg-blue-100 text-blue-800',
  INCOMING: 'bg-amber-100 text-amber-900',
  MUTUAL: 'bg-green-100 text-green-800',
}

interface InterestStatusBadgeProps {
  status: InterestStatus | undefined
  perspective: 'seeker' | 'employer'
}

export function InterestStatusBadge({ status, perspective }: InterestStatusBadgeProps) {
  const resolved = status ?? 'NONE'
  if (resolved === 'NONE') return null

  const label = perspective === 'seeker' ? SEEKER_LABELS[resolved] : EMPLOYER_LABELS[resolved]
  if (label == null) return null

  return (
    <span className={`rounded-full px-2.5 py-0.5 text-xs font-medium ${STATUS_STYLES[resolved]}`}>
      {label}
    </span>
  )
}
