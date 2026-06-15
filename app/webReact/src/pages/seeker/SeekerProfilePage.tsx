import { useEffect, useState } from 'react'
import {
  createEducation,
  createExperience,
  deleteEducation,
  deleteExperience,
  fetchEducation,
  fetchExperience,
  fetchSeekerProfile,
  fetchSeekerSkills,
  updateSeekerProfile,
  updateSeekerSkills,
} from '../../api/seekerApi'
import type {
  CreateSeekerEducationRequest,
  CreateSeekerExperienceRequest,
  SeekerEducationDto,
  SeekerExperienceDto,
  SeekerProfileDto,
} from '../../api/types'
import { EmptyState } from '../../components/EmptyState'
import { FormSection } from '../../components/FormSection'
import { SkillPicker } from '../../components/SkillPicker'

export function SeekerProfilePage() {
  const [profile, setProfile] = useState<SeekerProfileDto | null>(null)
  const [experience, setExperience] = useState<SeekerExperienceDto[]>([])
  const [education, setEducation] = useState<SeekerEducationDto[]>([])
  const [skillIds, setSkillIds] = useState<number[]>([])
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  const load = async () => {
    const [p, exp, edu, skills] = await Promise.all([
      fetchSeekerProfile(),
      fetchExperience(),
      fetchEducation(),
      fetchSeekerSkills(),
    ])
    setProfile(p)
    setExperience(exp)
    setEducation(edu)
    setSkillIds(skills.skillIds)
  }

  useEffect(() => {
    void load().catch((e: Error) => setError(e.message))
  }, [])

  async function saveProfile(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault()
    if (profile == null) return
    setSaving(true)
    setMessage(null)
    setError(null)
    try {
      const updated = await updateSeekerProfile({
        firstName: profile.firstName,
        middleName: profile.middleName,
        lastName: profile.lastName,
        phone: profile.phone,
        telegram: profile.telegram,
        linkedin: profile.linkedin,
      })
      setProfile(updated)
      setMessage('Профиль сохранён')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка сохранения')
    } finally {
      setSaving(false)
    }
  }

  async function saveSkills(ids: number[]) {
    setSkillIds(ids)
    try {
      await updateSeekerSkills(ids)
      setMessage('Навыки сохранены')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка сохранения навыков')
    }
  }

  async function handleAddExperience(body: CreateSeekerExperienceRequest) {
    const created = await createExperience(body)
    setExperience((prev) => [...prev, created])
  }

  async function handleAddEducation(body: CreateSeekerEducationRequest) {
    const created = await createEducation(body)
    setEducation((prev) => [...prev, created])
  }

  if (profile == null && error == null) {
    return <p className="text-sm text-neutral-500">Загрузка…</p>
  }

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="text-2xl font-semibold">Мой профиль</h1>
        <p className="mt-1 text-sm text-neutral-600">Опыт, навыки, образование и контакты</p>
      </div>
      {message != null && <p className="text-sm text-green-700">{message}</p>}
      {error != null && <p className="text-sm text-red-600">{error}</p>}

      {profile != null && (
        <form onSubmit={(e) => void saveProfile(e)}>
          <FormSection title="Личные данные и контакты">
            <div className="grid gap-4 sm:grid-cols-2">
              <label className="flex flex-col gap-1">
                <span className="text-sm font-medium">Имя</span>
                <input
                  required
                  value={profile.firstName}
                  onChange={(e) => setProfile({ ...profile, firstName: e.target.value })}
                  className="rounded-lg border border-neutral-300 px-3 py-2 text-sm"
                />
              </label>
              <label className="flex flex-col gap-1">
                <span className="text-sm font-medium">Отчество</span>
                <input
                  value={profile.middleName ?? ''}
                  onChange={(e) =>
                    setProfile({ ...profile, middleName: e.target.value || null })
                  }
                  className="rounded-lg border border-neutral-300 px-3 py-2 text-sm"
                />
              </label>
              <label className="flex flex-col gap-1">
                <span className="text-sm font-medium">Фамилия</span>
                <input
                  required
                  value={profile.lastName}
                  onChange={(e) => setProfile({ ...profile, lastName: e.target.value })}
                  className="rounded-lg border border-neutral-300 px-3 py-2 text-sm"
                />
              </label>
              <label className="flex flex-col gap-1">
                <span className="text-sm font-medium">Телефон</span>
                <input
                  value={profile.phone ?? ''}
                  onChange={(e) => setProfile({ ...profile, phone: e.target.value || null })}
                  className="rounded-lg border border-neutral-300 px-3 py-2 text-sm"
                />
              </label>
              <label className="flex flex-col gap-1">
                <span className="text-sm font-medium">Telegram</span>
                <input
                  value={profile.telegram ?? ''}
                  onChange={(e) => setProfile({ ...profile, telegram: e.target.value || null })}
                  className="rounded-lg border border-neutral-300 px-3 py-2 text-sm"
                />
              </label>
              <label className="flex flex-col gap-1">
                <span className="text-sm font-medium">LinkedIn</span>
                <input
                  value={profile.linkedin ?? ''}
                  onChange={(e) => setProfile({ ...profile, linkedin: e.target.value || null })}
                  className="rounded-lg border border-neutral-300 px-3 py-2 text-sm"
                />
              </label>
            </div>
            <button
              type="submit"
              disabled={saving}
              className="self-start rounded-lg bg-neutral-900 px-4 py-2 text-sm font-medium text-white disabled:opacity-50"
            >
              Сохранить
            </button>
          </FormSection>
        </form>
      )}

      <FormSection title="Навыки">
        <SkillPicker selectedIds={skillIds} onChange={(ids) => void saveSkills(ids)} />
      </FormSection>

      <FormSection title="Опыт работы">
        {experience.length === 0 ? (
          <EmptyState title="Опыт не добавлен" />
        ) : (
          <ul className="flex flex-col gap-3">
            {experience.map((item) => (
              <li key={item.id} className="rounded-lg border border-neutral-200 p-3">
                <div className="flex justify-between gap-2">
                  <div>
                    <p className="font-medium">{item.position}</p>
                    <p className="text-sm text-neutral-600">{item.companyName}</p>
                    <p className="text-xs text-neutral-500">
                      {item.startDate} — {item.endDate ?? 'настоящее время'}
                    </p>
                    {item.description != null && (
                      <p className="mt-1 text-sm text-neutral-700">{item.description}</p>
                    )}
                  </div>
                  <button
                    type="button"
                    onClick={() =>
                      void deleteExperience(item.id).then(() =>
                        setExperience((prev) => prev.filter((x) => x.id !== item.id)),
                      )
                    }
                    className="text-sm text-red-600"
                  >
                    Удалить
                  </button>
                </div>
              </li>
            ))}
          </ul>
        )}
        <ExperienceForm onAdd={(body) => void handleAddExperience(body)} />
      </FormSection>

      <FormSection title="Образование">
        {education.length === 0 ? (
          <EmptyState title="Образование не добавлено" />
        ) : (
          <ul className="flex flex-col gap-3">
            {education.map((item) => (
              <li key={item.id} className="rounded-lg border border-neutral-200 p-3">
                <div className="flex justify-between gap-2">
                  <div>
                    <p className="font-medium">{item.institution}</p>
                    <p className="text-sm text-neutral-600">{item.specialization}</p>
                    {item.degree != null && (
                      <p className="text-sm text-neutral-600">{item.degree}</p>
                    )}
                    <p className="text-xs text-neutral-500">Год окончания: {item.endYear}</p>
                  </div>
                  <button
                    type="button"
                    onClick={() =>
                      void deleteEducation(item.id).then(() =>
                        setEducation((prev) => prev.filter((x) => x.id !== item.id)),
                      )
                    }
                    className="text-sm text-red-600"
                  >
                    Удалить
                  </button>
                </div>
              </li>
            ))}
          </ul>
        )}
        <EducationForm onAdd={(body) => void handleAddEducation(body)} />
      </FormSection>
    </div>
  )
}

function ExperienceForm({
  onAdd,
}: {
  onAdd: (body: CreateSeekerExperienceRequest) => void
}) {
  const [companyName, setCompanyName] = useState('')
  const [position, setPosition] = useState('')
  const [startDate, setStartDate] = useState('')
  const [endDate, setEndDate] = useState('')

  function submit(e: React.FormEvent) {
    e.preventDefault()
    onAdd({
      companyName,
      position,
      startDate,
      endDate: endDate || null,
    })
    setCompanyName('')
    setPosition('')
    setStartDate('')
    setEndDate('')
  }

  return (
    <form onSubmit={submit} className="mt-2 grid gap-3 rounded-lg bg-neutral-50 p-4 sm:grid-cols-2">
      <input
        required
        placeholder="Компания"
        value={companyName}
        onChange={(e) => setCompanyName(e.target.value)}
        className="rounded-lg border border-neutral-300 px-3 py-2 text-sm"
      />
      <input
        required
        placeholder="Должность"
        value={position}
        onChange={(e) => setPosition(e.target.value)}
        className="rounded-lg border border-neutral-300 px-3 py-2 text-sm"
      />
      <input
        required
        type="date"
        value={startDate}
        onChange={(e) => setStartDate(e.target.value)}
        className="rounded-lg border border-neutral-300 px-3 py-2 text-sm"
      />
      <input
        type="date"
        value={endDate}
        onChange={(e) => setEndDate(e.target.value)}
        className="rounded-lg border border-neutral-300 px-3 py-2 text-sm"
      />
      <button
        type="submit"
        className="sm:col-span-2 self-start rounded-lg border border-neutral-900 px-4 py-2 text-sm"
      >
        Добавить опыт
      </button>
    </form>
  )
}

function EducationForm({
  onAdd,
}: {
  onAdd: (body: CreateSeekerEducationRequest) => void
}) {
  const [institution, setInstitution] = useState('')
  const [specialization, setSpecialization] = useState('')
  const [degree, setDegree] = useState('')
  const [endYear, setEndYear] = useState('')

  function submit(e: React.FormEvent) {
    e.preventDefault()
    onAdd({
      institution,
      specialization,
      degree: degree || null,
      endYear: Number(endYear),
    })
    setInstitution('')
    setSpecialization('')
    setDegree('')
    setEndYear('')
  }

  return (
    <form onSubmit={submit} className="mt-2 grid gap-3 rounded-lg bg-neutral-50 p-4 sm:grid-cols-2">
      <input
        required
        placeholder="Учебное заведение"
        value={institution}
        onChange={(e) => setInstitution(e.target.value)}
        className="rounded-lg border border-neutral-300 px-3 py-2 text-sm"
      />
      <input
        required
        placeholder="Специальность"
        value={specialization}
        onChange={(e) => setSpecialization(e.target.value)}
        className="rounded-lg border border-neutral-300 px-3 py-2 text-sm"
      />
      <input
        placeholder="Степень"
        value={degree}
        onChange={(e) => setDegree(e.target.value)}
        className="rounded-lg border border-neutral-300 px-3 py-2 text-sm"
      />
      <input
        required
        type="number"
        placeholder="Год окончания"
        value={endYear}
        onChange={(e) => setEndYear(e.target.value)}
        className="rounded-lg border border-neutral-300 px-3 py-2 text-sm"
      />
      <button
        type="submit"
        className="sm:col-span-2 self-start rounded-lg border border-neutral-900 px-4 py-2 text-sm"
      >
        Добавить образование
      </button>
    </form>
  )
}
