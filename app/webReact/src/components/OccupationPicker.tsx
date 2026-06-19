import {useEffect, useState} from 'react'
import {fetchOccupations} from '../api/referenceApi'
import type {OccupationDto} from '../api/types'
import {Spinner} from './Spinner'

interface OccupationPickerProps {
  selectedIds: number[]
  onChange: (ids: number[]) => void
  disabled?: boolean
  occupations?: OccupationDto[]
}

export function OccupationPicker({
  selectedIds,
  onChange,
  disabled,
  occupations: occupationsProp,
}: OccupationPickerProps) {
  const [occupations, setOccupations] = useState<OccupationDto[]>(occupationsProp ?? [])
  const [loading, setLoading] = useState(occupationsProp == null)

  useEffect(() => {
    if (occupationsProp != null) {
      setOccupations(occupationsProp)
      setLoading(false)
      return
    }
    setLoading(true)
    void fetchOccupations(true)
      .then(setOccupations)
      .finally(() => setLoading(false))
  }, [occupationsProp])

  function toggle(id: number) {
    if (disabled) return
    if (selectedIds.includes(id)) {
      onChange(selectedIds.filter((x) => x !== id))
    } else {
      onChange([...selectedIds, id])
    }
  }

  if (loading) {
    return (
      <div className="flex justify-center py-4">
        <Spinner size="sm" />
      </div>
    )
  }

  return (
    <div className="flex flex-wrap gap-2">
      {occupations.map((occ) => {
        const selected = selectedIds.includes(occ.id)
        return (
          <button
            key={occ.id}
            type="button"
            disabled={disabled}
            onClick={() => toggle(occ.id)}
            className={`rounded-full border px-3 py-1.5 text-sm transition ${
              selected
                ? 'border-neutral-900 bg-neutral-900 text-white'
                : 'border-neutral-300 bg-white text-neutral-700 hover:border-neutral-500'
            } disabled:opacity-50`}
          >
            {occ.name}
          </button>
        )
      })}
    </div>
  )
}
