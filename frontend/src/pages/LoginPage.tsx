import { useState } from 'react'
import { AdaptiveLayout } from '../components/AdaptiveLayout'
import { BrandTitle } from '../components/BrandTitle'

interface LoginPageProps {
  isBusy: boolean
  errorMessage: string | null
  onSignIn: (email: string) => void
}

export function LoginPage({ isBusy, errorMessage, onSignIn }: LoginPageProps) {
  const [email, setEmail] = useState('')

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (isBusy || email.trim() === '') return
    onSignIn(email)
  }

  return (
    <AdaptiveLayout>
      <form
        className="flex w-full flex-col items-center gap-4"
        onSubmit={handleSubmit}
        autoComplete="on"
      >
        <BrandTitle size="lg" />
        <p className="text-center text-base text-neutral-600">
          Найдите работу или наймите специалистов. Войдите, чтобы продолжить.
        </p>
        <label className="w-full" htmlFor="login-email">
          <span className="mb-1 block text-sm font-medium text-neutral-700">
            Электронная почта
          </span>
          <input
            id="login-email"
            name="email"
            type="email"
            inputMode="email"
            autoComplete="email"
            autoFocus
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            disabled={isBusy}
            className="w-full rounded-lg border border-neutral-300 bg-white px-3 py-2 text-base outline-none ring-neutral-900 focus:ring-2 disabled:opacity-60"
          />
        </label>
        {errorMessage != null && (
          <p className="w-full text-sm text-red-600">{errorMessage}</p>
        )}
        {isBusy ? (
          <div className="flex justify-center py-2">
            <div
              className="h-8 w-8 animate-spin rounded-full border-4 border-neutral-300 border-t-neutral-900"
              role="status"
              aria-label="Загрузка"
            />
          </div>
        ) : (
          <button
            type="submit"
            disabled={email.trim() === ''}
            className="w-full rounded-lg bg-neutral-900 px-4 py-2.5 text-sm font-medium text-white transition hover:bg-neutral-800 disabled:cursor-not-allowed disabled:opacity-50"
          >
            Продолжить
          </button>
        )}
        <p className="text-center text-xs text-neutral-500">
          Роль (соискатель или работодатель) выбирается один раз после входа и позже не
          меняется.
        </p>
      </form>
    </AdaptiveLayout>
  )
}
