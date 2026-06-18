interface MatchScoreBadgeProps {
  score: number
}

export function MatchScoreBadge({ score }: MatchScoreBadgeProps) {
  return (
    <div className="flex flex-col items-end gap-1">
      <div className="flex items-center gap-2">
        <div className="h-2 w-24 overflow-hidden rounded-full bg-neutral-200">
          <div
            className="h-full rounded-full bg-neutral-900"
            style={{ width: `${score}%` }}
          />
        </div>
        <span className="text-sm font-semibold tabular-nums">{score}/100</span>
      </div>
    </div>
  )
}
