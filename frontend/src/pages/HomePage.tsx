import {useTranslation} from 'react-i18next'
import type {AuthUserDto} from '../api/types'
import {AdaptiveLayout} from '../components/AdaptiveLayout'
import {displayRoleLabel} from '../lib/roleLabels'

interface HomePageProps {
  user: AuthUserDto
  onLogout: () => void
}

export function HomePage({user, onLogout}: HomePageProps) {
  const {t} = useTranslation()

  return (
    <AdaptiveLayout>
      <div className="flex flex-col gap-3">
        <h1 className="text-xl font-semibold">{t('home.loggedIn')}</h1>
        <p className="text-base text-neutral-700">
          {user.profileName != null && user.profileName !== ''
            ? `${user.profileName} · `
            : ''}
          {user.email} · {displayRoleLabel(user.role, t)}
        </p>
        <button
          type="button"
          onClick={onLogout}
          className="self-start rounded-lg bg-neutral-900 px-4 py-2 text-sm font-medium text-white transition hover:bg-neutral-800"
        >
          {t('home.logout')}
        </button>
      </div>
    </AdaptiveLayout>
  )
}
