import { LABEL_YOU_ON_SCALE } from './personalityLabels'

interface PersonalityTraitScaleProps {
  leftPole: string
  rightPole: string
  scalePosition: number
}

export function PersonalityTraitScale({
  leftPole,
  rightPole,
  scalePosition,
}: PersonalityTraitScaleProps) {
  const clamped = Math.min(1, Math.max(0, scalePosition))
  const markerPercent = clamped * 100

  return (
    <div className="mt-4">
      <div className="mb-2 flex justify-between text-sm font-medium text-neutral-800">
        <span>{leftPole}</span>
        <span>{rightPole}</span>
      </div>
      <div className="relative h-2 rounded-full bg-neutral-200">
        <div
          className="absolute top-1/2 h-4 w-4 -translate-x-1/2 -translate-y-1/2 rounded-full border-2 border-white bg-neutral-900 shadow-sm"
          style={{ left: `${markerPercent}%` }}
          aria-hidden
        />
      </div>
      <div className="relative mt-2 h-5">
        <span
          className="absolute -translate-x-1/2 text-xs text-neutral-500"
          style={{ left: `${markerPercent}%` }}
        >
          {LABEL_YOU_ON_SCALE}
        </span>
      </div>
    </div>
  )
}
