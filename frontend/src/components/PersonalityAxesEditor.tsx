import {useTranslation} from 'react-i18next'
import type {PersonalityAxesDto} from '../api/types'
import {AXIS_KEYS, axisLabel} from './personality/personalityLabels'

interface PersonalityAxesEditorProps {
  value: PersonalityAxesDto
  onChange: (value: PersonalityAxesDto) => void
}

export function PersonalityAxesEditor({value, onChange}: PersonalityAxesEditorProps) {
  const {t} = useTranslation()

  return (
    <div className="flex flex-col gap-3">
      <p className="text-sm text-muted">{t('components.personalityAxesEditor.hint')}</p>
      {AXIS_KEYS.map((key) => (
        <label key={key} className="flex flex-col gap-1.5">
          <div className="flex items-center justify-between text-sm">
            <span className="font-medium text-foreground">{axisLabel(key, t)}</span>
            <span className="tabular-nums font-semibold text-brand-600">
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
            className="w-full accent-brand-600"
          />
        </label>
      ))}
    </div>
  )
}
