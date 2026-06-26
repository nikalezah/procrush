import {useEffect, useState} from 'react'
import {searchSkills} from '../api/referenceApi'
import type {SkillDto} from '../api/types'

interface SkillPickerProps {
  selectedIds: number[]
  onChange: (ids: number[]) => void
  disabled?: boolean
}

export function SkillPicker({selectedIds, onChange, disabled}: SkillPickerProps) {
  const [query, setQuery] = useState('')
  const [skills, setSkills] = useState<SkillDto[]>([])
  const [selectedSkills, setSelectedSkills] = useState<SkillDto[]>([])

  useEffect(() => {
    void searchSkills(query || undefined).then(setSkills)
  }, [query])

  useEffect(() => {
    if (selectedIds.length === 0) {
      setSelectedSkills([])
      return
    }
    void searchSkills().then((all) => {
      setSelectedSkills(all.filter((s) => selectedIds.includes(s.id)))
    })
  }, [selectedIds])

  function addSkill(skill: SkillDto) {
    if (disabled || selectedIds.includes(skill.id)) return
    onChange([...selectedIds, skill.id])
  }

  function removeSkill(id: number) {
    if (disabled) return
    onChange(selectedIds.filter((x) => x !== id))
  }

  return (
    <div className="flex flex-col gap-3">
      <div className="flex flex-wrap gap-2">
        {selectedSkills.map((skill) => (
          <span
            key={skill.id}
            className="inline-flex items-center gap-1 rounded-full bg-brand-50 px-3 py-1 text-sm font-medium text-brand-800"
          >
            {skill.name}
            {!disabled && (
              <button
                type="button"
                onClick={() => removeSkill(skill.id)}
                className="text-brand-500 hover:text-brand-700"
                aria-label={`Удалить ${skill.name}`}
              >
                ×
              </button>
            )}
          </span>
        ))}
      </div>
      <input
        type="text"
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        disabled={disabled}
        placeholder="Поиск навыков…"
        className="w-full rounded-2xl border border-brand-200 bg-white px-4 py-2.5 text-sm outline-none focus:border-brand-300 focus:ring-2 focus:ring-brand-200 disabled:opacity-50"
      />
      <div className="flex max-h-40 flex-wrap gap-2 overflow-y-auto">
        {skills
          .filter((s) => !selectedIds.includes(s.id))
          .slice(0, 20)
          .map((skill) => (
            <button
              key={skill.id}
              type="button"
              disabled={disabled}
              onClick={() => addSkill(skill)}
              className="rounded-full border border-brand-200 px-3 py-1 text-sm text-stone-700 hover:border-brand-300 hover:bg-brand-50 disabled:opacity-50"
            >
              + {skill.name}
            </button>
          ))}
      </div>
    </div>
  )
}
