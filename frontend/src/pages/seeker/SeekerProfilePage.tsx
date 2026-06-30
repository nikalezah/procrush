import {useEffect, useState} from 'react'
import {useTranslation} from 'react-i18next'
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
import {EmptyState} from '../../components/EmptyState'
import {FormSection} from '../../components/FormSection'
import {SkillPicker} from '../../components/SkillPicker'
import {Alert} from '../../components/ui/Alert'
import {Button} from '../../components/ui/Button'
import {PageHeader} from '../../components/ui/PageHeader'
import {Spinner} from '../../components/Spinner'
import {resolveError} from '../../i18n/resolveApiError'

export function SeekerProfilePage() {
  const {t} = useTranslation()
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
    void load().catch((err) => setError(resolveError(err)))
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
      setMessage(t('seeker.profile.saved'))
    } catch (err) {
      setError(resolveError(err) || t('common.saveError'))
    } finally {
      setSaving(false)
    }
  }

  async function saveSkills(ids: number[]) {
    setSkillIds(ids)
    try {
      await updateSeekerSkills(ids)
      setMessage(t('seeker.profile.skillsSaved'))
    } catch (err) {
      setError(resolveError(err) || t('seeker.profile.skillsSaveError'))
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
    return (
      <div className="flex justify-center py-16">
        <Spinner />
      </div>
    )
  }

  return (
    <div className="flex flex-col gap-6">
      <PageHeader title={t('seeker.profile.title')} subtitle={t('seeker.profile.subtitle')} />
      {message != null && <Alert variant="success">{message}</Alert>}
      {error != null && <Alert variant="error">{error}</Alert>}

      {profile != null && (
        <form onSubmit={(e) => void saveProfile(e)}>
          <FormSection title={t('seeker.profile.personalAndContacts')}>
            <div className="grid gap-4 sm:grid-cols-2">
              <label className="flex flex-col gap-1">
                <span className="text-sm font-medium">{t('common.fields.firstName')}</span>
                <input
                  required
                  value={profile.firstName}
                  onChange={(e) => setProfile({ ...profile, firstName: e.target.value })}
                  className="rounded-2xl border border-border bg-surface px-4 py-2.5 text-sm text-foreground focus:border-brand-300 focus:ring-2 focus:ring-brand-200 dark:focus:ring-brand-900 outline-none"
                />
              </label>
              <label className="flex flex-col gap-1">
                <span className="text-sm font-medium">{t('common.fields.middleName')}</span>
                <input
                  value={profile.middleName ?? ''}
                  onChange={(e) =>
                    setProfile({ ...profile, middleName: e.target.value || null })
                  }
                  className="rounded-2xl border border-border bg-surface px-4 py-2.5 text-sm text-foreground focus:border-brand-300 focus:ring-2 focus:ring-brand-200 dark:focus:ring-brand-900 outline-none"
                />
              </label>
              <label className="flex flex-col gap-1">
                <span className="text-sm font-medium">{t('common.fields.lastName')}</span>
                <input
                  required
                  value={profile.lastName}
                  onChange={(e) => setProfile({ ...profile, lastName: e.target.value })}
                  className="rounded-2xl border border-border bg-surface px-4 py-2.5 text-sm text-foreground focus:border-brand-300 focus:ring-2 focus:ring-brand-200 dark:focus:ring-brand-900 outline-none"
                />
              </label>
              <label className="flex flex-col gap-1">
                <span className="text-sm font-medium">{t('common.fields.phone')}</span>
                <input
                  value={profile.phone ?? ''}
                  onChange={(e) => setProfile({ ...profile, phone: e.target.value || null })}
                  className="rounded-2xl border border-border bg-surface px-4 py-2.5 text-sm text-foreground focus:border-brand-300 focus:ring-2 focus:ring-brand-200 dark:focus:ring-brand-900 outline-none"
                />
              </label>
              <label className="flex flex-col gap-1">
                <span className="text-sm font-medium">Telegram</span>
                <input
                  value={profile.telegram ?? ''}
                  onChange={(e) => setProfile({ ...profile, telegram: e.target.value || null })}
                  className="rounded-2xl border border-border bg-surface px-4 py-2.5 text-sm text-foreground focus:border-brand-300 focus:ring-2 focus:ring-brand-200 dark:focus:ring-brand-900 outline-none"
                />
              </label>
              <label className="flex flex-col gap-1">
                <span className="text-sm font-medium">LinkedIn</span>
                <input
                  value={profile.linkedin ?? ''}
                  onChange={(e) => setProfile({ ...profile, linkedin: e.target.value || null })}
                  className="rounded-2xl border border-border bg-surface px-4 py-2.5 text-sm text-foreground focus:border-brand-300 focus:ring-2 focus:ring-brand-200 dark:focus:ring-brand-900 outline-none"
                />
              </label>
            </div>
            <Button type="submit" disabled={saving}>
              {saving ? t('common.saving') : t('common.save')}
            </Button>
          </FormSection>
        </form>
      )}

      <FormSection title={t('seeker.profile.skills')}>
        <SkillPicker selectedIds={skillIds} onChange={(ids) => void saveSkills(ids)} />
      </FormSection>

      <FormSection title={t('seeker.profile.experience.title')}>
        {experience.length === 0 ? (
          <EmptyState title={t('seeker.profile.experience.empty')} />
        ) : (
          <ul className="flex flex-col gap-3">
            {experience.map((item) => (
              <li key={item.id} className="rounded-2xl border border-border-subtle bg-surface-muted p-4">
                <div className="flex justify-between gap-2">
                  <div>
                    <p className="font-semibold text-foreground">{item.position}</p>
                    <p className="text-sm text-muted">{item.companyName}</p>
                    <p className="text-xs text-muted">
                      {item.startDate} — {item.endDate ?? t('common.present')}
                    </p>
                    {item.description != null && (
                      <p className="mt-1 text-sm text-muted">{item.description}</p>
                    )}
                  </div>
                  <button
                    type="button"
                    onClick={() =>
                      void deleteExperience(item.id).then(() =>
                        setExperience((prev) => prev.filter((x) => x.id !== item.id)),
                      )
                    }
                    className="text-sm text-red-600 hover:text-red-700"
                  >
                    {t('common.delete')}
                  </button>
                </div>
              </li>
            ))}
          </ul>
        )}
        <ExperienceForm onAdd={(body) => void handleAddExperience(body)} />
      </FormSection>

      <FormSection title={t('seeker.profile.education.title')}>
        {education.length === 0 ? (
          <EmptyState title={t('seeker.profile.education.empty')} />
        ) : (
          <ul className="flex flex-col gap-3">
            {education.map((item) => (
              <li key={item.id} className="rounded-2xl border border-border-subtle bg-surface-muted p-4">
                <div className="flex justify-between gap-2">
                  <div>
                    <p className="font-semibold text-foreground">{item.institution}</p>
                    <p className="text-sm text-muted">{item.specialization}</p>
                    {item.degree != null && (
                      <p className="text-sm text-muted">{item.degree}</p>
                    )}
                    <p className="text-xs text-muted">
                      {t('seeker.profile.education.graduationYear', {year: item.endYear})}
                    </p>
                  </div>
                  <button
                    type="button"
                    onClick={() =>
                      void deleteEducation(item.id).then(() =>
                        setEducation((prev) => prev.filter((x) => x.id !== item.id)),
                      )
                    }
                    className="text-sm text-red-600 hover:text-red-700"
                  >
                    {t('common.delete')}
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
  const {t} = useTranslation()
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
    <form onSubmit={submit} className="mt-2 grid gap-3 rounded-2xl border border-border-subtle bg-surface-muted p-4 sm:grid-cols-2">
      <input
        required
        placeholder={t('seeker.profile.experience.placeholders.company')}
        value={companyName}
        onChange={(e) => setCompanyName(e.target.value)}
        className="rounded-2xl border border-border-subtle bg-surface px-4 py-2.5 text-sm text-foreground outline-none focus:border-brand-300 focus:ring-2 focus:ring-brand-200 dark:focus:ring-brand-900"
      />
      <input
        required
        placeholder={t('seeker.profile.experience.placeholders.position')}
        value={position}
        onChange={(e) => setPosition(e.target.value)}
        className="rounded-2xl border border-border-subtle bg-surface px-4 py-2.5 text-sm text-foreground outline-none focus:border-brand-300 focus:ring-2 focus:ring-brand-200 dark:focus:ring-brand-900"
      />
      <input
        required
        type="date"
        value={startDate}
        onChange={(e) => setStartDate(e.target.value)}
        className="rounded-2xl border border-border-subtle bg-surface px-4 py-2.5 text-sm text-foreground outline-none focus:border-brand-300 focus:ring-2 focus:ring-brand-200 dark:focus:ring-brand-900"
      />
      <input
        type="date"
        value={endDate}
        onChange={(e) => setEndDate(e.target.value)}
        className="rounded-2xl border border-border-subtle bg-surface px-4 py-2.5 text-sm text-foreground outline-none focus:border-brand-300 focus:ring-2 focus:ring-brand-200 dark:focus:ring-brand-900"
      />
      <button
        type="submit"
        className="sm:col-span-2 self-start rounded-full gradient-brand px-5 py-2 text-sm font-medium text-white shadow-sm"
      >
        {t('seeker.profile.experience.addButton')}
      </button>
    </form>
  )
}

function EducationForm({
  onAdd,
}: {
  onAdd: (body: CreateSeekerEducationRequest) => void
}) {
  const {t} = useTranslation()
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
    <form onSubmit={submit} className="mt-2 grid gap-3 rounded-2xl border border-border-subtle bg-surface-muted p-4 sm:grid-cols-2">
      <input
        required
        placeholder={t('seeker.profile.education.placeholders.institution')}
        value={institution}
        onChange={(e) => setInstitution(e.target.value)}
        className="rounded-2xl border border-border-subtle bg-surface px-4 py-2.5 text-sm text-foreground outline-none focus:border-brand-300 focus:ring-2 focus:ring-brand-200 dark:focus:ring-brand-900"
      />
      <input
        required
        placeholder={t('seeker.profile.education.placeholders.specialization')}
        value={specialization}
        onChange={(e) => setSpecialization(e.target.value)}
        className="rounded-2xl border border-border-subtle bg-surface px-4 py-2.5 text-sm text-foreground outline-none focus:border-brand-300 focus:ring-2 focus:ring-brand-200 dark:focus:ring-brand-900"
      />
      <input
        placeholder={t('seeker.profile.education.placeholders.degree')}
        value={degree}
        onChange={(e) => setDegree(e.target.value)}
        className="rounded-2xl border border-border-subtle bg-surface px-4 py-2.5 text-sm text-foreground outline-none focus:border-brand-300 focus:ring-2 focus:ring-brand-200 dark:focus:ring-brand-900"
      />
      <input
        required
        type="number"
        placeholder={t('seeker.profile.education.placeholders.endYear')}
        value={endYear}
        onChange={(e) => setEndYear(e.target.value)}
        className="rounded-2xl border border-border-subtle bg-surface px-4 py-2.5 text-sm text-foreground outline-none focus:border-brand-300 focus:ring-2 focus:ring-brand-200 dark:focus:ring-brand-900"
      />
      <button
        type="submit"
        className="sm:col-span-2 self-start rounded-full gradient-brand px-5 py-2 text-sm font-medium text-white shadow-sm"
      >
        {t('seeker.profile.education.addButton')}
      </button>
    </form>
  )
}
