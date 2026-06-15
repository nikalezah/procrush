import { useState } from 'react'
import type { AuthUserDto, CompleteRegistrationRequest, UserRole } from '../api/types'
import { AdaptiveLayout } from '../components/AdaptiveLayout'
import { RoleToggle } from '../components/RoleToggle'

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
      <form className="flex w-full flex-col gap-4" onSubmit={handleSubmit} autoComplete="on">
        <h1 className="text-center text-xl font-semibold">Завершите регистрацию</h1>
        <p className="text-center text-sm text-neutral-600">
          Добро пожаловать. Выберите тип аккаунта для {user.email} — его нельзя
          будет изменить позже.
        </p>
        <p className="text-center text-sm text-neutral-500">
          Эти данные можно изменить в любое время.
        </p>

        <RoleToggle value={role} onChange={setRole} disabled={isBusy} />

        {role === 'SEEKER' ? (
          <div className="grid gap-4 sm:grid-cols-2">
            <label className="flex flex-col gap-1">
              <span className="text-sm font-medium text-neutral-700">Имя</span>
              <input
                name="given-name"
                autoComplete="given-name"
                required
                value={firstName}
                onChange={(e) => setFirstName(e.target.value)}
                disabled={isBusy}
                className="rounded-lg border border-neutral-300 px-3 py-2 text-sm disabled:bg-neutral-50"
              />
            </label>
            <label className="flex flex-col gap-1">
              <span className="text-sm font-medium text-neutral-700">Фамилия</span>
              <input
                name="family-name"
                autoComplete="family-name"
                required
                value={lastName}
                onChange={(e) => setLastName(e.target.value)}
                disabled={isBusy}
                className="rounded-lg border border-neutral-300 px-3 py-2 text-sm disabled:bg-neutral-50"
              />
            </label>
            <label className="flex flex-col gap-1 sm:col-span-2">
              <span className="text-sm font-medium text-neutral-700">Отчество</span>
              <input
                name="additional-name"
                autoComplete="additional-name"
                value={middleName}
                onChange={(e) => setMiddleName(e.target.value)}
                disabled={isBusy}
                className="rounded-lg border border-neutral-300 px-3 py-2 text-sm disabled:bg-neutral-50"
              />
            </label>
          </div>
        ) : (
          <label className="flex flex-col gap-1">
            <span className="text-sm font-medium text-neutral-700">Название компании</span>
            <input
              name="organization"
              autoComplete="organization"
              required
              value={companyName}
              onChange={(e) => setCompanyName(e.target.value)}
              disabled={isBusy}
              className="rounded-lg border border-neutral-300 px-3 py-2 text-sm disabled:bg-neutral-50"
            />
          </label>
        )}

        {errorMessage != null && (
          <p className="w-full text-sm text-red-600">{errorMessage}</p>
        )}

        <button
          type="submit"
          disabled={isBusy}
          className="w-full rounded-lg bg-neutral-900 px-4 py-2.5 text-sm font-medium text-white transition hover:bg-neutral-800 disabled:cursor-not-allowed disabled:opacity-60"
        >
          {isBusy ? 'Создание аккаунта…' : 'Создать аккаунт'}
        </button>
      </form>
    </AdaptiveLayout>
  )
}
