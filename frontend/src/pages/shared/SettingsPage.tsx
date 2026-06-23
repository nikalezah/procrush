import { useEffect, useState } from 'react'
import { fetchEmployerProfile, updateEmployerProfile } from '../../api/employerApi'
import { fetchSeekerProfile, updateSeekerProfile } from '../../api/seekerApi'
import type { EmployerProfileDto, SeekerProfileDto } from '../../api/types'
import { FormSection } from '../../components/FormSection'
import { LoadingSpinner } from '../../components/LoadingSpinner'
import { useAuth } from '../../hooks/useAuth'

export function SettingsPage() {
  const { state, refreshSession, deleteAccount, isBusy } = useAuth()
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
  const { user } = state

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
      <div>
        <h1 className="text-2xl font-semibold">Настройки</h1>
        <p className="mt-1 text-sm text-neutral-600">Управление аккаунтом</p>
      </div>

      <FormSection title={user.role === 'SEEKER' ? 'Личные данные' : 'Компания'}>
        {profileLoading ? (
          <p className="text-sm text-neutral-500">Загрузка…</p>
        ) : (
          <form className="flex flex-col gap-4" onSubmit={(e) => void saveProfile(e)}>
            {user.role === 'SEEKER' && seekerProfile != null && (
              <div className="grid gap-4 sm:grid-cols-2">
                <label className="flex flex-col gap-1">
                  <span className="text-sm font-medium text-neutral-700">Имя</span>
                  <input
                    required
                    value={seekerProfile.firstName}
                    onChange={(e) =>
                      setSeekerProfile({ ...seekerProfile, firstName: e.target.value })
                    }
                    className="rounded-lg border border-neutral-300 px-3 py-2 text-sm"
                  />
                </label>
                <label className="flex flex-col gap-1">
                  <span className="text-sm font-medium text-neutral-700">Фамилия</span>
                  <input
                    required
                    value={seekerProfile.lastName}
                    onChange={(e) =>
                      setSeekerProfile({ ...seekerProfile, lastName: e.target.value })
                    }
                    className="rounded-lg border border-neutral-300 px-3 py-2 text-sm"
                  />
                </label>
                <label className="flex flex-col gap-1 sm:col-span-2">
                  <span className="text-sm font-medium text-neutral-700">Отчество</span>
                  <input
                    value={seekerProfile.middleName ?? ''}
                    onChange={(e) =>
                      setSeekerProfile({
                        ...seekerProfile,
                        middleName: e.target.value || null,
                      })
                    }
                    className="rounded-lg border border-neutral-300 px-3 py-2 text-sm"
                  />
                </label>
              </div>
            )}
            {user.role === 'EMPLOYER' && employerProfile != null && (
              <>
                <label className="flex flex-col gap-1">
                  <span className="text-sm font-medium text-neutral-700">Название</span>
                  <input
                    required
                    value={employerProfile.name}
                    onChange={(e) =>
                      setEmployerProfile({ ...employerProfile, name: e.target.value })
                    }
                    className="rounded-lg border border-neutral-300 px-3 py-2 text-sm"
                  />
                </label>
                <label className="flex flex-col gap-1">
                  <span className="text-sm font-medium text-neutral-700">Описание</span>
                  <textarea
                    rows={4}
                    value={employerProfile.description ?? ''}
                    onChange={(e) =>
                      setEmployerProfile({
                        ...employerProfile,
                        description: e.target.value || null,
                      })
                    }
                    className="rounded-lg border border-neutral-300 px-3 py-2 text-sm"
                  />
                </label>
                <label className="flex flex-col gap-1">
                  <span className="text-sm font-medium text-neutral-700">Сайт</span>
                  <input
                    value={employerProfile.website ?? ''}
                    onChange={(e) =>
                      setEmployerProfile({
                        ...employerProfile,
                        website: e.target.value || null,
                      })
                    }
                    className="rounded-lg border border-neutral-300 px-3 py-2 text-sm"
                  />
                </label>
                <label className="flex flex-col gap-1">
                  <span className="text-sm font-medium text-neutral-700">Телефон</span>
                  <input
                    value={employerProfile.phone ?? ''}
                    onChange={(e) =>
                      setEmployerProfile({
                        ...employerProfile,
                        phone: e.target.value || null,
                      })
                    }
                    className="rounded-lg border border-neutral-300 px-3 py-2 text-sm"
                  />
                </label>
                <label className="flex flex-col gap-1">
                  <span className="text-sm font-medium text-neutral-700">Email для связи</span>
                  <input
                    type="email"
                    value={employerProfile.emailContact ?? ''}
                    onChange={(e) =>
                      setEmployerProfile({
                        ...employerProfile,
                        emailContact: e.target.value || null,
                      })
                    }
                    className="rounded-lg border border-neutral-300 px-3 py-2 text-sm"
                  />
                </label>
              </>
            )}
            {profileMessage != null && <p className="text-sm text-green-700">{profileMessage}</p>}
            {profileError != null && <p className="text-sm text-red-600">{profileError}</p>}
            <button
              type="submit"
              disabled={profileSaving || profileLoading}
              className="self-start rounded-lg bg-neutral-900 px-4 py-2 text-sm font-medium text-white disabled:cursor-not-allowed disabled:opacity-60"
            >
              {profileSaving ? 'Сохранение…' : 'Сохранить'}
            </button>
          </form>
        )}
      </FormSection>

      <FormSection title="Электронная почта" description="Раздел в разработке">
        <label className="flex flex-col gap-1">
          <span className="text-sm font-medium text-neutral-700">Текущий адрес</span>
          <input
            type="email"
            value={user.email}
            readOnly
            className="rounded-lg border border-neutral-200 bg-neutral-50 px-3 py-2 text-sm text-neutral-600"
          />
        </label>
        <label className="flex flex-col gap-1">
          <span className="text-sm font-medium text-neutral-700">Новый адрес</span>
          <input
            type="email"
            disabled
            placeholder="example@mail.ru"
            className="rounded-lg border border-neutral-200 bg-neutral-50 px-3 py-2 text-sm text-neutral-500 disabled:cursor-not-allowed"
          />
        </label>
        <button
          type="button"
          disabled
          className="self-start rounded-lg bg-neutral-200 px-4 py-2 text-sm text-neutral-500"
        >
          Сохранить (скоро)
        </button>
      </FormSection>
      <FormSection title="Пароль" description="Раздел в разработке">
        <label className="flex flex-col gap-1">
          <span className="text-sm font-medium text-neutral-700">Текущий пароль</span>
          <input
            type="password"
            disabled
            className="rounded-lg border border-neutral-200 bg-neutral-50 px-3 py-2 text-sm text-neutral-500 disabled:cursor-not-allowed"
          />
        </label>
        <label className="flex flex-col gap-1">
          <span className="text-sm font-medium text-neutral-700">Новый пароль</span>
          <input
            type="password"
            disabled
            className="rounded-lg border border-neutral-200 bg-neutral-50 px-3 py-2 text-sm text-neutral-500 disabled:cursor-not-allowed"
          />
        </label>
        <label className="flex flex-col gap-1">
          <span className="text-sm font-medium text-neutral-700">Подтверждение пароля</span>
          <input
            type="password"
            disabled
            className="rounded-lg border border-neutral-200 bg-neutral-50 px-3 py-2 text-sm text-neutral-500 disabled:cursor-not-allowed"
          />
        </label>
        <button
          type="button"
          disabled
          className="self-start rounded-lg bg-neutral-200 px-4 py-2 text-sm text-neutral-500"
        >
          Изменить пароль (скоро)
        </button>
      </FormSection>
      <FormSection
        title="Удаление аккаунта"
        description="Необратимое удаление всех данных"
      >
        <p className="text-sm text-neutral-600">
          Удаление аккаунта необратимо. Будут удалены профиль, сессии, опыт работы, образование,
          навыки, опросы и все остальные связанные данные.
        </p>
        {deleteError != null && <p className="text-sm text-red-600">{deleteError}</p>}
        {!deleteConfirmOpen ? (
          <button
            type="button"
            disabled={isBusy}
            onClick={() => setDeleteConfirmOpen(true)}
            className="self-start rounded-lg border border-red-300 bg-red-50 px-4 py-2 text-sm font-medium text-red-700 hover:bg-red-100 disabled:cursor-not-allowed disabled:opacity-60"
          >
            Удалить аккаунт
          </button>
        ) : (
          <div className="flex flex-col gap-3 rounded-lg border border-red-200 bg-red-50 p-4">
            <p className="text-sm font-medium text-red-900">
              Вы уверены? Это действие нельзя отменить.
            </p>
            <div className="flex flex-wrap gap-2">
              <button
                type="button"
                disabled={isBusy}
                onClick={() => void handleDeleteAccount()}
                className="rounded-lg bg-red-600 px-4 py-2 text-sm font-medium text-white hover:bg-red-700 disabled:cursor-not-allowed disabled:opacity-60"
              >
                {isBusy ? 'Удаление…' : 'Да, удалить навсегда'}
              </button>
              <button
                type="button"
                disabled={isBusy}
                onClick={() => setDeleteConfirmOpen(false)}
                className="rounded-lg border border-neutral-300 bg-white px-4 py-2 text-sm hover:bg-neutral-50 disabled:cursor-not-allowed disabled:opacity-60"
              >
                Отмена
              </button>
            </div>
          </div>
        )}
      </FormSection>
    </div>
  )
}
