import { Link } from 'react-router-dom'
import { useEffect, useState } from 'react'
import {
  createJobProfile,
  deleteJobProfile,
  fetchJobProfiles,
} from '../../api/employerApi'
import { fetchOccupations } from '../../api/referenceApi'
import type { CreateJobProfileRequest, JobProfileDto, OccupationDto } from '../../api/types'
import { EmptyState } from '../../components/EmptyState'
import { FormSection } from '../../components/FormSection'
import { SkillPicker } from '../../components/SkillPicker'

export function EmployerProfilesPage() {
  const [profiles, setProfiles] = useState<JobProfileDto[]>([])
  const [occupations, setOccupations] = useState<OccupationDto[]>([])
  const [error, setError] = useState<string | null>(null)
  const [showForm, setShowForm] = useState(false)

  useEffect(() => {
    void Promise.all([fetchJobProfiles(), fetchOccupations(true)])
      .then(([p, o]) => {
        setProfiles(p)
        setOccupations(o)
      })
      .catch((e: Error) => setError(e.message))
  }, [])

  async function handleCreate(body: CreateJobProfileRequest) {
    const created = await createJobProfile(body)
    setProfiles((prev) => [...prev, created])
    setShowForm(false)
  }

  async function handleDelete(id: number) {
    await deleteJobProfile(id)
    setProfiles((prev) => prev.filter((p) => p.id !== id))
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold">Профили кандидатов</h1>
          <p className="mt-1 text-sm text-neutral-600">
            Опишите идеальных сотрудников для ваших позиций
          </p>
        </div>
        <button
          type="button"
          onClick={() => setShowForm((v) => !v)}
          className="rounded-lg bg-neutral-900 px-4 py-2 text-sm font-medium text-white"
        >
          {showForm ? 'Отмена' : 'Создать профиль'}
        </button>
      </div>
      {error != null && <p className="text-sm text-red-600">{error}</p>}

      {showForm && (
        <JobProfileForm occupations={occupations} onSubmit={(body) => void handleCreate(body)} />
      )}

      {profiles.length === 0 ? (
        <EmptyState
          title="Профили не созданы"
          description="Создайте первый профиль идеального кандидата"
        />
      ) : (
        <ul className="flex flex-col gap-3">
          {profiles.map((profile) => (
            <li
              key={profile.id}
              className="rounded-xl border border-neutral-200 bg-white p-4"
            >
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <h3 className="font-medium">{profile.occupationName}</h3>
                  {profile.description != null && (
                    <p className="mt-1 text-sm text-neutral-700">{profile.description}</p>
                  )}
                  <div className="mt-2 flex flex-wrap gap-1">
                    {profile.skills.map((s) => (
                      <span
                        key={s.id}
                        className="rounded-full bg-neutral-100 px-2 py-0.5 text-xs"
                      >
                        {s.name}
                      </span>
                    ))}
                  </div>
                  <p className="mt-2 text-xs text-neutral-500">
                    {profile.isActive ? 'Активен' : 'Неактивен'}
                  </p>
                </div>
                <div className="flex flex-col gap-2">
                  <Link
                    to={`/employer/profiles/${profile.id}/candidates`}
                    className="rounded-lg border border-neutral-300 px-3 py-1.5 text-sm text-center hover:bg-neutral-50"
                  >
                    Кандидаты
                  </Link>
                  <button
                    type="button"
                    onClick={() => void handleDelete(profile.id)}
                    className="text-sm text-red-600"
                  >
                    Удалить
                  </button>
                </div>
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

function JobProfileForm({
  occupations,
  onSubmit,
}: {
  occupations: OccupationDto[]
  onSubmit: (body: CreateJobProfileRequest) => void
}) {
  const [occupationId, setOccupationId] = useState<number>(occupations[0]?.id ?? 0)
  const [description, setDescription] = useState('')
  const [skillIds, setSkillIds] = useState<number[]>([])

  function submit(e: React.FormEvent) {
    e.preventDefault()
    onSubmit({ occupationId, description: description || null, skillIds, isActive: true })
  }

  return (
    <FormSection title="Новый профиль">
      <form onSubmit={submit} className="flex flex-col gap-4">
        <label className="flex flex-col gap-1">
          <span className="text-sm font-medium">Должность</span>
          <select
            required
            value={occupationId}
            onChange={(e) => setOccupationId(Number(e.target.value))}
            className="rounded-lg border border-neutral-300 px-3 py-2 text-sm"
          >
            {occupations.map((o) => (
              <option key={o.id} value={o.id}>
                {o.name}
              </option>
            ))}
          </select>
        </label>
        <label className="flex flex-col gap-1">
          <span className="text-sm font-medium">Описание</span>
          <textarea
            rows={3}
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            className="rounded-lg border border-neutral-300 px-3 py-2 text-sm"
          />
        </label>
        <div>
          <span className="text-sm font-medium">Навыки</span>
          <div className="mt-2">
            <SkillPicker selectedIds={skillIds} onChange={setSkillIds} />
          </div>
        </div>
        <button
          type="submit"
          className="self-start rounded-lg bg-neutral-900 px-4 py-2 text-sm font-medium text-white"
        >
          Сохранить профиль
        </button>
      </form>
    </FormSection>
  )
}
