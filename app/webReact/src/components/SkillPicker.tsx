import { useEffect, useState } from 'react'
import { searchSkills } from '../api/referenceApi'
import type { SkillDto } from '../api/types'

interface SkillPickerProps {
  selectedIds: number[]
  onChange: (ids: number[]) => void
  disabled?: boolean
}

export function SkillPicker({ selectedIds, onChange, disabled }: SkillPickerProps) {
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
            className="inline-flex items-center gap-1 rounded-full bg-neutral-100 px-3 py-1 text-sm"
          >
            {skill.name}
            {!disabled && (
              <button
                type="button"
                onClick={() => removeSkill(skill.id)}
                className="text-neutral-500 hover:text-neutral-900"
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
        className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-neutral-900 disabled:opacity-50"
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
              className="rounded-full border border-neutral-300 px-3 py-1 text-sm hover:border-neutral-500 disabled:opacity-50"
            >
              + {skill.name}
            </button>
          ))}
      </div>
    </div>
  )
}
