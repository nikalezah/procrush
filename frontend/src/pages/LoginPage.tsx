import {useState} from 'react'
import {useTranslation} from 'react-i18next'
import {AdaptiveLayout} from '../components/AdaptiveLayout'
import {Spinner} from '../components/Spinner'
import {Button} from '../components/ui/Button'
import {Input} from '../components/ui/Input'

interface LoginPageProps {
  isBusy: boolean
  errorMessage: string | null
  onSignIn: (email: string) => void
}

export function LoginPage({isBusy, errorMessage, onSignIn}: LoginPageProps) {
  const {t} = useTranslation()
  const [email, setEmail] = useState('')

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (isBusy || email.trim() === '') return
    onSignIn(email)
  }

  return (
    <AdaptiveLayout>
      <form className="flex w-full flex-col gap-5" onSubmit={handleSubmit} autoComplete="on">
        <div className="text-center">
          <p className="text-lg font-medium text-foreground">{t('auth.login.title')}</p>
          <p className="mt-1 text-sm text-muted">{t('auth.login.subtitle')}</p>
        </div>

        <Input
          label={t('auth.login.emailLabel')}
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
          error={errorMessage ?? undefined}
        />

        {isBusy ? (
          <div className="flex justify-center py-3">
            <Spinner />
          </div>
        ) : (
          <Button type="submit" fullWidth size="lg" disabled={email.trim() === ''}>
            {t('auth.login.submit')}
          </Button>
        )}

        <p className="text-center text-xs text-muted">{t('auth.login.roleHint')}</p>
      </form>
    </AdaptiveLayout>
  )
}
