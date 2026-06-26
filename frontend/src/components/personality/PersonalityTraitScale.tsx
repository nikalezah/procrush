import {LABEL_YOU_ON_SCALE} from './personalityLabels'

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
      <div className="mb-2 flex justify-between text-sm font-medium text-stone-800">
        <span>{leftPole}</span>
        <span>{rightPole}</span>
      </div>
      <div className="relative h-2.5 rounded-full bg-brand-100">
        <div
          className="gradient-brand absolute top-1/2 h-5 w-5 -translate-x-1/2 -translate-y-1/2 rounded-full border-2 border-white shadow-md shadow-brand-500/30"
          style={{left: `${markerPercent}%`}}
          aria-hidden
        />
      </div>
      <div className="relative mt-2 h-5">
        <span
          className="absolute -translate-x-1/2 text-xs font-medium text-brand-600"
          style={{left: `${markerPercent}%`}}
        >
          {LABEL_YOU_ON_SCALE}
        </span>
      </div>
    </div>
  )
}
