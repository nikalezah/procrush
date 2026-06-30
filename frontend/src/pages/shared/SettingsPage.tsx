import {useEffect, useState} from 'react'
import {useTranslation} from 'react-i18next'
import {fetchEmployerProfile, updateEmployerProfile} from '../../api/employerApi'
import {fetchSeekerProfile, updateSeekerProfile} from '../../api/seekerApi'
import type {EmployerProfileDto, SeekerProfileDto} from '../../api/types'
import {FormSection} from '../../components/FormSection'
import {LoadingSpinner} from '../../components/LoadingSpinner'
import {ThemeToggle} from '../../components/ThemeToggle'
import {Alert} from '../../components/ui/Alert'
import {Avatar} from '../../components/ui/Avatar'
import {Button} from '../../components/ui/Button'
import {Input, TextArea} from '../../components/ui/Input'
import {PageHeader} from '../../components/ui/PageHeader'
import {Spinner} from '../../components/Spinner'
import {displayRoleLabel} from '../../lib/roleLabels'
import {useAuth} from '../../hooks/useAuth'
import {applyDocumentLocale} from '../../i18n/detectLocale'
import i18n from '../../i18n/config'
import {resolveError} from '../../i18n/resolveApiError'
import {type AppLocale, setStoredLocale} from '../../i18n/localeStorage'

export function SettingsPage() {
  const {t} = useTranslation()
  const {state, refreshSession, deleteAccount, signOut, isBusy} = useAuth()
  const [seekerProfile, setSeekerProfile] = useState<SeekerProfileDto | null>(null)
  const [employerProfile, setEmployerProfile] = useState<EmployerProfileDto | null>(null)
  const [profileLoading, setProfileLoading] = useState(true)
  const [profileSaving, setProfileSaving] = useState(false)
  const [profileMessage, setProfileMessage] = useState<string | null>(null)
  const [profileError, setProfileError] = useState<string | null>(null)
  const [deleteError, setDeleteError] = useState<string | null>(null)
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false)
  const [locale, setLocale] = useState<AppLocale>(i18n.language as AppLocale)
  const role = state.kind === 'authenticated' ? state.user.role : null

  useEffect(() => {
    const onLanguageChanged = (lng: string) => setLocale(lng as AppLocale)
    i18n.on('languageChanged', onLanguageChanged)
    return () => i18n.off('languageChanged', onLanguageChanged)
  }, [])

  useEffect(() => {
    if (role == null) return
    setProfileLoading(true)
    setProfileError(null)
    const load =
      role === 'SEEKER'
        ? fetchSeekerProfile().then(setSeekerProfile)
        : fetchEmployerProfile().then(setEmployerProfile)
    void load
      .catch((err) => setProfileError(resolveError(err)))
      .finally(() => setProfileLoading(false))
  }, [role])

  if (state.kind !== 'authenticated') return <LoadingSpinner />
  const {user} = state
  const displayName = user.profileName ?? user.email

  function handleLanguageChange(nextLocale: AppLocale) {
    setStoredLocale(nextLocale)
    void i18n.changeLanguage(nextLocale)
    applyDocumentLocale(nextLocale)
  }

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
      setProfileMessage(
        user.role === 'EMPLOYER' ? t('settings.companySaved') : t('settings.profileSaved'),
      )
    } catch (err) {
      setProfileError(resolveError(err) || t('common.saveError'))
    } finally {
      setProfileSaving(false)
    }
  }

  async function handleDeleteAccount() {
    setDeleteError(null)
    try {
      await deleteAccount()
    } catch (err) {
      setDeleteError(resolveError(err) || t('settings.deleteAccountError'))
      setDeleteConfirmOpen(false)
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <PageHeader title={t('settings.title')} subtitle={t('settings.subtitle')} />

      <FormSection title={t('settings.loggedInAs')}>
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-center gap-4">
            <Avatar name={displayName} size="lg" />
            <div>
              {user.profileName != null && user.profileName !== '' && (
                <p className="font-semibold text-foreground">{user.profileName}</p>
              )}
              <p className="text-sm text-muted">{user.email}</p>
              <p className="mt-0.5 text-xs font-medium text-brand-600">
                {displayRoleLabel(user.role, t)}
              </p>
            </div>
          </div>
          <Button variant="secondary" onClick={() => void signOut()} className="shrink-0">
            {t('common.logout')}
          </Button>
        </div>
      </FormSection>

      <FormSection
        title={t('settings.language.title')}
        description={t('settings.language.description')}
      >
        <div className="flex flex-wrap gap-2">
          {(['ru', 'en'] as const).map((option) => (
            <Button
              key={option}
              type="button"
              variant={locale === option ? 'primary' : 'secondary'}
              onClick={() => handleLanguageChange(option)}
            >
              {t(`settings.language.${option}`)}
            </Button>
          ))}
        </div>
      </FormSection>

      <FormSection
        title={t('settings.appearance.title')}
        description={t('settings.appearance.description')}
      >
        <ThemeToggle variant="full" />
      </FormSection>

      <FormSection
        title={user.role === 'SEEKER' ? t('settings.personalData') : t('settings.company')}
      >
        {profileLoading ? (
          <div className="flex justify-center py-6">
            <Spinner />
          </div>
        ) : (
          <form className="flex flex-col gap-4" onSubmit={(e) => void saveProfile(e)}>
            {user.role === 'SEEKER' && seekerProfile != null && (
              <div className="grid gap-4 sm:grid-cols-2">
                <Input
                  label={t('common.fields.firstName')}
                  required
                  value={seekerProfile.firstName}
                  onChange={(e) =>
                    setSeekerProfile({...seekerProfile, firstName: e.target.value})
                  }
                />
                <Input
                  label={t('common.fields.lastName')}
                  required
                  value={seekerProfile.lastName}
                  onChange={(e) =>
                    setSeekerProfile({...seekerProfile, lastName: e.target.value})
                  }
                />
                <div className="sm:col-span-2">
                  <Input
                    label={t('common.fields.middleName')}
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
                  label={t('common.fields.companyTitle')}
                  required
                  value={employerProfile.name}
                  onChange={(e) =>
                    setEmployerProfile({...employerProfile, name: e.target.value})
                  }
                />
                <TextArea
                  label={t('common.fields.description')}
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
                  label={t('common.fields.website')}
                  value={employerProfile.website ?? ''}
                  onChange={(e) =>
                    setEmployerProfile({
                      ...employerProfile,
                      website: e.target.value || null,
                    })
                  }
                />
                <Input
                  label={t('common.fields.phone')}
                  value={employerProfile.phone ?? ''}
                  onChange={(e) =>
                    setEmployerProfile({
                      ...employerProfile,
                      phone: e.target.value || null,
                    })
                  }
                />
                <Input
                  label={t('common.fields.emailContact')}
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
              {profileSaving ? t('common.saving') : t('common.save')}
            </Button>
          </form>
        )}
      </FormSection>

      <FormSection
        title={t('settings.emailSection.title')}
        description={t('settings.emailSection.description')}
      >
        <Input
          label={t('settings.emailSection.currentAddress')}
          type="email"
          value={user.email}
          readOnly
          disabled
        />
        <Input
          label={t('settings.emailSection.newAddress')}
          type="email"
          disabled
          placeholder={t('settings.emailSection.newAddressPlaceholder')}
        />
        <Button variant="secondary" disabled>
          {t('settings.emailSection.saveSoon')}
        </Button>
      </FormSection>

      <FormSection
        title={t('settings.passwordSection.title')}
        description={t('settings.passwordSection.description')}
      >
        <Input label={t('settings.passwordSection.currentPassword')} type="password" disabled />
        <Input label={t('settings.passwordSection.newPassword')} type="password" disabled />
        <Input label={t('settings.passwordSection.confirmPassword')} type="password" disabled />
        <Button variant="secondary" disabled>
          {t('settings.passwordSection.changeSoon')}
        </Button>
      </FormSection>

      <FormSection
        title={t('settings.deleteSection.title')}
        description={t('settings.deleteSection.description')}
      >
        <p className="text-sm text-muted">{t('settings.deleteSection.warning')}</p>
        {deleteError != null && <Alert variant="error">{deleteError}</Alert>}
        {!deleteConfirmOpen ? (
          <Button variant="danger" disabled={isBusy} onClick={() => setDeleteConfirmOpen(true)}>
            {t('settings.deleteSection.deleteButton')}
          </Button>
        ) : (
          <Alert variant="danger" title={t('settings.deleteSection.confirmTitle')}>
            <div className="mt-3 flex flex-wrap gap-2">
              <Button
                variant="danger"
                disabled={isBusy}
                onClick={() => void handleDeleteAccount()}
              >
                {isBusy ? t('settings.deleteSection.deleting') : t('settings.deleteSection.confirmButton')}
              </Button>
              <Button
                variant="secondary"
                disabled={isBusy}
                onClick={() => setDeleteConfirmOpen(false)}
              >
                {t('common.cancel')}
              </Button>
            </div>
          </Alert>
        )}
      </FormSection>
    </div>
  )
}
