import {useTranslation} from 'react-i18next'

interface MatchScoreBadgeProps {
  score: number
  size?: 'sm' | 'md'
}

function scoreColor(score: number): string {
  if (score >= 75) return 'text-accent-700'
  if (score >= 50) return 'text-brand-600'
  return 'text-amber-600'
}

function scoreGradient(score: number): string {
  if (score >= 75) return 'from-accent-400 to-accent-600'
  if (score >= 50) return 'from-brand-400 to-brand-600'
  return 'from-amber-400 to-amber-500'
}

export function MatchScoreBadge({score, size = 'md'}: MatchScoreBadgeProps) {
  const {t} = useTranslation()
  const ringSize = size === 'sm' ? 'h-12 w-12' : 'h-16 w-16'
  const textSize = size === 'sm' ? 'text-sm' : 'text-lg'

  return (
    <div className="flex flex-col items-center gap-1">
      <div
        className={`relative flex ${ringSize} items-center justify-center rounded-full bg-gradient-to-br ${scoreGradient(score)} p-0.5`}
      >
        <div className="flex h-full w-full flex-col items-center justify-center rounded-full bg-surface">
          <span className={`font-bold tabular-nums ${textSize} ${scoreColor(score)}`}>{score}%</span>
        </div>
      </div>
      <span className="text-xs font-medium text-muted">{t('components.matchScore.label')}</span>
    </div>
  )
}
