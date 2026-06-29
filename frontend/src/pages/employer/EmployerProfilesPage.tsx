import {Link} from 'react-router-dom'
import {useEffect, useState} from 'react'
import {useTranslation} from 'react-i18next'
import {createJobProfile, deleteJobProfile, fetchJobProfiles, updateJobProfile} from '../../api/employerApi'
import {fetchOccupations} from '../../api/referenceApi'
import type {CreateJobProfileRequest, JobProfileDto, OccupationDto, PersonalityAxesDto} from '../../api/types'
import {DEFAULT_PERSONALITY_AXES} from '../../api/types'
import {EmptyState} from '../../components/EmptyState'
import {FormSection} from '../../components/FormSection'
import {PersonalityAxesEditor} from '../../components/PersonalityAxesEditor'
import {AXIS_KEYS, axisLabel} from '../../components/personality/personalityLabels'
import {SkillPicker} from '../../components/SkillPicker'
import {Spinner} from '../../components/Spinner'
import {Alert} from '../../components/ui/Alert'
import {Button} from '../../components/ui/Button'
import {Card} from '../../components/ui/Card'
import {PageHeader} from '../../components/ui/PageHeader'
import {Select, TextArea} from '../../components/ui/Input'
import {resolveError} from '../../i18n/resolveApiError'

export function EmployerProfilesPage() {
  const {t} = useTranslation()
  const [profiles, setProfiles] = useState<JobProfileDto[]>([])
  const [occupations, setOccupations] = useState<OccupationDto[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [showForm, setShowForm] = useState(false)
  const [editingProfile, setEditingProfile] = useState<JobProfileDto | null>(null)

  useEffect(() => {
    setLoading(true)
    void Promise.all([fetchJobProfiles(), fetchOccupations(true)])
      .then(([p, o]) => {
        setProfiles(p)
        setOccupations(o)
      })
      .catch((err) => setError(resolveError(err)))
      .finally(() => setLoading(false))
  }, [])

  async function handleCreate(body: CreateJobProfileRequest) {
    const created = await createJobProfile(body)
    setProfiles((prev) => [...prev, created])
    setShowForm(false)
  }

  async function handleUpdate(id: number, body: CreateJobProfileRequest) {
    const updated = await updateJobProfile(id, body)
    setProfiles((prev) => prev.map((p) => (p.id === id ? updated : p)))
    setEditingProfile(null)
  }

  async function handleDelete(id: number) {
    await deleteJobProfile(id)
    setProfiles((prev) => prev.filter((p) => p.id !== id))
  }

  if (loading) {
    return (
      <div className="flex justify-center py-16">
        <Spinner />
      </div>
    )
  }

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title={t('employer.profiles.title')}
        subtitle={t('employer.profiles.subtitle')}
        action={
          <Button
            size="sm"
            variant={showForm ? 'secondary' : 'primary'}
            onClick={() => {
              setEditingProfile(null)
              setShowForm((v) => !v)
            }}
          >
            {showForm ? t('common.cancel') : t('employer.profiles.create')}
          </Button>
        }
      />
      {error != null && <Alert variant="error">{error}</Alert>}

      {showForm && editingProfile == null && (
        <JobProfileForm occupations={occupations} onSubmit={(body) => void handleCreate(body)} />
      )}

      {editingProfile != null && (
        <JobProfileForm
          key={editingProfile.id}
          occupations={occupations}
          initialProfile={editingProfile}
          onSubmit={(body) => void handleUpdate(editingProfile.id, body)}
          onCancel={() => setEditingProfile(null)}
        />
      )}

      {profiles.length === 0 ? (
        <EmptyState
          title={t('employer.profiles.emptyTitle')}
          description={t('employer.profiles.emptyDescription')}
          icon="💼"
        />
      ) : (
        <ul className="flex flex-col gap-4">
          {profiles.map((profile) => (
            <li key={profile.id}>
              <Card>
                <div className="flex flex-wrap items-start justify-between gap-4">
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-2">
                      <h3 className="text-lg font-semibold text-stone-900">{profile.occupationName}</h3>
                      <span
                        className={`rounded-full px-2.5 py-0.5 text-xs font-medium ${
                          profile.isActive
                            ? 'bg-emerald-100 text-emerald-700'
                            : 'bg-stone-100 text-stone-500'
                        }`}
                      >
                        {profile.isActive
                          ? t('employer.profiles.status.active')
                          : t('employer.profiles.status.inactive')}
                      </span>
                    </div>
                    {profile.description != null && (
                      <p className="mt-2 text-sm text-stone-600">{profile.description}</p>
                    )}
                    <div className="mt-3 flex flex-wrap gap-1.5">
                      {profile.skills.map((s) => (
                        <span
                          key={s.id}
                          className="rounded-full bg-brand-50 px-2.5 py-0.5 text-xs font-medium text-brand-700"
                        >
                          {s.name}
                        </span>
                      ))}
                    </div>
                    <div className="mt-2 flex flex-wrap gap-1.5">
                      {AXIS_KEYS.slice(0, 3).map((key) => (
                        <span
                          key={key}
                          className="rounded-full bg-stone-50 px-2 py-0.5 text-xs text-stone-500"
                        >
                          {axisLabel(key, t)}: {Math.round(profile.personalityAxes[key] * 100)}%
                        </span>
                      ))}
                    </div>
                  </div>
                  <div className="flex flex-col gap-2">
                    <Link to={`/employer/profiles/${profile.id}/candidates`}>
                      <Button size="sm" fullWidth>
                        {t('employer.profiles.candidatesButton')}
                      </Button>
                    </Link>
                    <Button
                      size="sm"
                      variant="secondary"
                      fullWidth
                      onClick={() => {
                        setShowForm(false)
                        setEditingProfile(profile)
                      }}
                    >
                      {t('employer.profiles.edit')}
                    </Button>
                    <Button
                      size="sm"
                      variant="danger"
                      fullWidth
                      onClick={() => void handleDelete(profile.id)}
                    >
                      {t('common.delete')}
                    </Button>
                  </div>
                </div>
              </Card>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

function JobProfileForm({
  occupations,
  initialProfile,
  onSubmit,
  onCancel,
}: {
  occupations: OccupationDto[]
  initialProfile?: JobProfileDto
  onSubmit: (body: CreateJobProfileRequest) => void
  onCancel?: () => void
}) {
  const {t} = useTranslation()
  const isEditing = initialProfile != null
  const [occupationId, setOccupationId] = useState<number>(
    initialProfile?.occupationId ?? occupations[0]?.id ?? 0,
  )
  const [description, setDescription] = useState(initialProfile?.description ?? '')
  const [skillIds, setSkillIds] = useState<number[]>(initialProfile?.skillIds ?? [])
  const [isActive, setIsActive] = useState(initialProfile?.isActive ?? true)
  const [personalityAxes, setPersonalityAxes] = useState<PersonalityAxesDto>(
    initialProfile?.personalityAxes ?? DEFAULT_PERSONALITY_AXES,
  )

  function submit(e: React.FormEvent) {
    e.preventDefault()
    onSubmit({
      occupationId,
      description: description || null,
      skillIds,
      isActive,
      personalityAxes,
    })
  }

  return (
    <FormSection
      title={isEditing ? t('employer.profiles.form.editTitle') : t('employer.profiles.form.createTitle')}
    >
      <form onSubmit={submit} className="flex flex-col gap-4">
        <Select
          label={t('common.fields.occupation')}
          required
          value={occupationId}
          onChange={(e) => setOccupationId(Number(e.target.value))}
        >
          {occupations.map((o) => (
            <option key={o.id} value={o.id}>
              {o.name}
            </option>
          ))}
        </Select>
        <TextArea
          label={t('common.fields.description')}
          rows={3}
          value={description}
          onChange={(e) => setDescription(e.target.value)}
        />
        <div>
          <span className="text-sm font-medium text-stone-700">{t('common.fields.skills')}</span>
          <div className="mt-2">
            <SkillPicker selectedIds={skillIds} onChange={setSkillIds} />
          </div>
        </div>
        <div>
          <span className="text-sm font-medium text-stone-700">
            {t('employer.profiles.form.personalityTraits')}
          </span>
          <div className="mt-2">
            <PersonalityAxesEditor value={personalityAxes} onChange={setPersonalityAxes} />
          </div>
        </div>
        <label className="flex items-center gap-2 text-sm text-stone-700">
          <input
            type="checkbox"
            checked={isActive}
            onChange={(e) => setIsActive(e.target.checked)}
            className="rounded border-brand-300 text-brand-600 focus:ring-brand-200"
          />
          {t('employer.profiles.form.activeCheckbox')}
        </label>
        <div className="flex flex-wrap gap-2">
          <Button type="submit">
            {isEditing ? t('common.save') : t('employer.profiles.form.createSubmit')}
          </Button>
          {onCancel != null && (
            <Button type="button" variant="secondary" onClick={onCancel}>
              {t('common.cancel')}
            </Button>
          )}
        </div>
      </form>
    </FormSection>
  )
}
