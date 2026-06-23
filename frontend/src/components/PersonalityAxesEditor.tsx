import type {PersonalityAxesDto} from '../api/types'
import {AXIS_KEYS, AXIS_LABELS} from './personality/personalityLabels'

interface PersonalityAxesEditorProps {
  value: PersonalityAxesDto
  onChange: (value: PersonalityAxesDto) => void
}

export function PersonalityAxesEditor({ value, onChange }: PersonalityAxesEditorProps) {
  return (
    <div className="flex flex-col gap-3">
      <p className="text-sm text-neutral-600">
        Укажите желаемый уровень по каждой личностной оси (0–100%)
      </p>
      {AXIS_KEYS.map((key) => (
        <label key={key} className="flex flex-col gap-1">
          <div className="flex items-center justify-between text-sm">
            <span className="font-medium">{AXIS_LABELS[key]}</span>
            <span className="tabular-nums text-neutral-600">
              {Math.round(value[key] * 100)}%
            </span>
          </div>
          <input
            type="range"
            min={0}
            max={100}
            value={Math.round(value[key] * 100)}
            onChange={(e) =>
              onChange({
                ...value,
                [key]: Number(e.target.value) / 100,
              })
            }
            className="w-full"
          />
        </label>
      ))}
    </div>
  )
}
