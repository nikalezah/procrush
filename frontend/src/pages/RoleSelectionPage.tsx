import {useState} from 'react'
import {useTranslation} from 'react-i18next'
import type {AuthUserDto, CompleteRegistrationRequest, UserRole} from '../api/types'
import {AdaptiveLayout} from '../components/AdaptiveLayout'
import {RoleToggle} from '../components/RoleToggle'
import {Button} from '../components/ui/Button'
import {Input} from '../components/ui/Input'

interface RoleSelectionPageProps {
  user: AuthUserDto
  isBusy: boolean
  errorMessage: string | null
  onCompleteRegistration: (request: CompleteRegistrationRequest) => void
}

export function RoleSelectionPage({
  user,
  isBusy,
  errorMessage,
  onCompleteRegistration,
}: RoleSelectionPageProps) {
  const {t} = useTranslation()
  const [role, setRole] = useState<UserRole>('SEEKER')
  const [firstName, setFirstName] = useState('')
  const [lastName, setLastName] = useState('')
  const [middleName, setMiddleName] = useState('')
  const [companyName, setCompanyName] = useState('')

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (isBusy) return
    if (role === 'SEEKER') {
      onCompleteRegistration({
        email: user.email,
        role,
        firstName: firstName.trim(),
        lastName: lastName.trim(),
        middleName: middleName.trim() || null,
      })
      return
    }
    onCompleteRegistration({
      email: user.email,
      role,
      companyName: companyName.trim(),
    })
  }

  return (
    <AdaptiveLayout>
      <form className="flex w-full flex-col gap-5" onSubmit={handleSubmit} autoComplete="on">
        <div className="text-center">
          <h1 className="text-xl font-bold text-stone-900">{t('auth.roleSelection.title')}</h1>
          <p className="mt-2 text-sm text-stone-500">
            {t('auth.roleSelection.welcome', {email: user.email})}
          </p>
        </div>

        <RoleToggle value={role} onChange={setRole} disabled={isBusy} />

        {role === 'SEEKER' ? (
          <div className="grid gap-4 sm:grid-cols-2">
            <Input
              label={t('common.fields.firstName')}
              name="given-name"
              autoComplete="given-name"
              required
              value={firstName}
              onChange={(e) => setFirstName(e.target.value)}
              disabled={isBusy}
            />
            <Input
              label={t('common.fields.lastName')}
              name="family-name"
              autoComplete="family-name"
              required
              value={lastName}
              onChange={(e) => setLastName(e.target.value)}
              disabled={isBusy}
            />
            <div className="sm:col-span-2">
              <Input
                label={t('common.fields.middleName')}
                name="additional-name"
                autoComplete="additional-name"
                value={middleName}
                onChange={(e) => setMiddleName(e.target.value)}
                disabled={isBusy}
                hint={t('common.optional')}
              />
            </div>
          </div>
        ) : (
          <Input
            label={t('common.fields.companyName')}
            name="organization"
            autoComplete="organization"
            required
            value={companyName}
            onChange={(e) => setCompanyName(e.target.value)}
            disabled={isBusy}
          />
        )}

        {errorMessage != null && <p className="text-sm text-red-600">{errorMessage}</p>}

        <Button type="submit" fullWidth size="lg" disabled={isBusy}>
          {isBusy ? t('auth.roleSelection.creating') : t('auth.roleSelection.submit')}
        </Button>
      </form>
    </AdaptiveLayout>
  )
}
