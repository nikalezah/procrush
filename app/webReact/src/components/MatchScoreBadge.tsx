interface MatchScoreBadgeProps {
  score: number
  testsCompleted: number
  isScoreReduced: boolean
}

export function MatchScoreBadge({ score, testsCompleted, isScoreReduced }: MatchScoreBadgeProps) {
  return (
    <div className="flex flex-col items-end gap-1">
      <div className="flex items-center gap-1">
        {Array.from({ length: 5 }, (_, i) => (
          <span
            key={i}
            className={`h-2.5 w-2.5 rounded-full ${
              i < score ? 'bg-neutral-900' : 'bg-neutral-200'
            }`}
          />
        ))}
        <span className="ml-1 text-sm font-semibold">{score}/5</span>
      </div>
      {isScoreReduced && (
        <span
          className="text-xs text-amber-700"
          title={`Оценка занижена: пройдено ${testsCompleted} из 3 тестов`}
        >
          Оценка занижена · тестов {testsCompleted}/3
        </span>
      )}
    </div>
  )
}
