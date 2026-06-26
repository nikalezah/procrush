import {useEffect, useState} from 'react'
import {fetchEmployerProfile, updateEmployerProfile} from '../../api/employerApi'
import {fetchSeekerProfile, updateSeekerProfile} from '../../api/seekerApi'
import type {EmployerProfileDto, SeekerProfileDto} from '../../api/types'
import {FormSection} from '../../components/FormSection'
import {LoadingSpinner} from '../../components/LoadingSpinner'
import {Alert} from '../../components/ui/Alert'
import {Avatar} from '../../components/ui/Avatar'
import {Button} from '../../components/ui/Button'
import {Input, TextArea} from '../../components/ui/Input'
import {PageHeader} from '../../components/ui/PageHeader'
import {Spinner} from '../../components/Spinner'
import {displayRoleLabel} from '../../lib/roleLabels'
import {useAuth} from '../../hooks/useAuth'

export function SettingsPage() {
  const {state, refreshSession, deleteAccount, signOut, isBusy} = useAuth()
  const [seekerProfile, setSeekerProfile] = useState<SeekerProfileDto | null>(null)
  const [employerProfile, setEmployerProfile] = useState<EmployerProfileDto | null>(null)
  const [profileLoading, setProfileLoading] = useState(true)
  const [profileSaving, setProfileSaving] = useState(false)
  const [profileMessage, setProfileMessage] = useState<string | null>(null)
  const [profileError, setProfileError] = useState<string | null>(null)
  const [deleteError, setDeleteError] = useState<string | null>(null)
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false)
  const role = state.kind === 'authenticated' ? state.user.role : null

  useEffect(() => {
    if (role == null) return
    setProfileLoading(true)
    setProfileError(null)
    const load =
      role === 'SEEKER'
        ? fetchSeekerProfile().then(setSeekerProfile)
        : fetchEmployerProfile().then(setEmployerProfile)
    void load
      .catch((e: Error) => setProfileError(e.message))
      .finally(() => setProfileLoading(false))
  }, [role])

  if (state.kind !== 'authenticated') return <LoadingSpinner />
  const {user} = state
  const displayName = user.profileName ?? user.email

  async function saveProfile(e: React.FormEvent) {
    e.preventDefault()
    setProfileSaving(true)
    setProfileMessage(null)
    setProfileError(null)
    try {
      if (user.role === 'SEEKER' && seekerProfile != null) {
        const updated = await updateSeekerProfile({
          firstName: seekerProfile.firstName,
          middleName: seekerProfile.middleName,
          lastName: seekerProfile.lastName,
          phone: seekerProfile.phone,
          telegram: seekerProfile.telegram,
          linkedin: seekerProfile.linkedin,
        })
        setSeekerProfile(updated)
      } else if (user.role === 'EMPLOYER' && employerProfile != null) {
        const updated = await updateEmployerProfile({
          name: employerProfile.name,
          description: employerProfile.description,
          website: employerProfile.website,
          phone: employerProfile.phone,
          emailContact: employerProfile.emailContact,
        })
        setEmployerProfile(updated)
      }
      await refreshSession()
      setProfileMessage(user.role === 'EMPLOYER' ? 'Данные компании сохранены' : 'Профиль сохранён')
    } catch (err) {
      setProfileError(err instanceof Error ? err.message : 'Ошибка сохранения')
    } finally {
      setProfileSaving(false)
    }
  }

  async function handleDeleteAccount() {
    setDeleteError(null)
    try {
      await deleteAccount()
    } catch (err) {
      setDeleteError(err instanceof Error ? err.message : 'Не удалось удалить аккаунт')
      setDeleteConfirmOpen(false)
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <PageHeader title="Аккаунт" subtitle="Вход, данные и управление аккаунтом" />

      <FormSection title="Вы вошли как">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-center gap-4">
            <Avatar name={displayName} size="lg" />
            <div>
              {user.profileName != null && user.profileName !== '' && (
                <p className="font-semibold text-stone-900">{user.profileName}</p>
              )}
              <p className="text-sm text-stone-500">{user.email}</p>
              <p className="mt-0.5 text-xs font-medium text-brand-600">
                {displayRoleLabel(user.role)}
              </p>
            </div>
          </div>
          <Button variant="secondary" onClick={() => void signOut()} className="shrink-0">
            Выйти
          </Button>
        </div>
      </FormSection>

      <FormSection title={user.role === 'SEEKER' ? 'Личные данные' : 'Компания'}>
        {profileLoading ? (
          <div className="flex justify-center py-6">
            <Spinner />
          </div>
        ) : (
          <form className="flex flex-col gap-4" onSubmit={(e) => void saveProfile(e)}>
            {user.role === 'SEEKER' && seekerProfile != null && (
              <div className="grid gap-4 sm:grid-cols-2">
                <Input
                  label="Имя"
                  required
                  value={seekerProfile.firstName}
                  onChange={(e) =>
                    setSeekerProfile({...seekerProfile, firstName: e.target.value})
                  }
                />
                <Input
                  label="Фамилия"
                  required
                  value={seekerProfile.lastName}
                  onChange={(e) =>
                    setSeekerProfile({...seekerProfile, lastName: e.target.value})
                  }
                />
                <div className="sm:col-span-2">
                  <Input
                    label="Отчество"
                    value={seekerProfile.middleName ?? ''}
                    onChange={(e) =>
                      setSeekerProfile({
                        ...seekerProfile,
                        middleName: e.target.value || null,
                      })
                    }
                  />
                </div>
              </div>
            )}
            {user.role === 'EMPLOYER' && employerProfile != null && (
              <>
                <Input
                  label="Название"
                  required
                  value={employerProfile.name}
                  onChange={(e) =>
                    setEmployerProfile({...employerProfile, name: e.target.value})
                  }
                />
                <TextArea
                  label="Описание"
                  rows={4}
                  value={employerProfile.description ?? ''}
                  onChange={(e) =>
                    setEmployerProfile({
                      ...employerProfile,
                      description: e.target.value || null,
                    })
                  }
                />
                <Input
                  label="Сайт"
                  value={employerProfile.website ?? ''}
                  onChange={(e) =>
                    setEmployerProfile({
                      ...employerProfile,
                      website: e.target.value || null,
                    })
                  }
                />
                <Input
                  label="Телефон"
                  value={employerProfile.phone ?? ''}
                  onChange={(e) =>
                    setEmployerProfile({
                      ...employerProfile,
                      phone: e.target.value || null,
                    })
                  }
                />
                <Input
                  label="Email для связи"
                  type="email"
                  value={employerProfile.emailContact ?? ''}
                  onChange={(e) =>
                    setEmployerProfile({
                      ...employerProfile,
                      emailContact: e.target.value || null,
                    })
                  }
                />
              </>
            )}
            {profileMessage != null && <Alert variant="success">{profileMessage}</Alert>}
            {profileError != null && <Alert variant="error">{profileError}</Alert>}
            <Button type="submit" disabled={profileSaving || profileLoading}>
              {profileSaving ? 'Сохранение…' : 'Сохранить'}
            </Button>
          </form>
        )}
      </FormSection>

      <FormSection title="Электронная почта" description="Раздел в разработке">
        <Input label="Текущий адрес" type="email" value={user.email} readOnly disabled />
        <Input label="Новый адрес" type="email" disabled placeholder="example@mail.ru" />
        <Button variant="secondary" disabled>
          Сохранить (скоро)
        </Button>
      </FormSection>

      <FormSection title="Пароль" description="Раздел в разработке">
        <Input label="Текущий пароль" type="password" disabled />
        <Input label="Новый пароль" type="password" disabled />
        <Input label="Подтверждение пароля" type="password" disabled />
        <Button variant="secondary" disabled>
          Изменить пароль (скоро)
        </Button>
      </FormSection>

      <FormSection title="Удаление аккаунта" description="Необратимое удаление всех данных">
        <p className="text-sm text-stone-600">
          Удаление аккаунта необратимо. Будут удалены профиль, сессии, опыт работы, образование,
          навыки, опросы и все остальные связанные данные.
        </p>
        {deleteError != null && <Alert variant="error">{deleteError}</Alert>}
        {!deleteConfirmOpen ? (
          <Button variant="danger" disabled={isBusy} onClick={() => setDeleteConfirmOpen(true)}>
            Удалить аккаунт
          </Button>
        ) : (
          <Alert variant="error" title="Вы уверены? Это действие нельзя отменить.">
            <div className="mt-3 flex flex-wrap gap-2">
              <Button
                variant="danger"
                disabled={isBusy}
                onClick={() => void handleDeleteAccount()}
              >
                {isBusy ? 'Удаление…' : 'Да, удалить навсегда'}
              </Button>
              <Button
                variant="secondary"
                disabled={isBusy}
                onClick={() => setDeleteConfirmOpen(false)}
              >
                Отмена
              </Button>
            </div>
          </Alert>
        )}
      </FormSection>
    </div>
  )
}
