import {NavLink, Outlet} from 'react-router-dom'
import {useTranslation} from 'react-i18next'
import type {AuthUserDto, UserRole} from '../api/types'
import {displayRoleLabel} from '../lib/roleLabels'
import {Avatar} from './ui/Avatar'
import {BrandTitle} from './BrandTitle'
import {NavBadge} from './NavBadge'

export interface NavItem {
  to: string
  label: string
  end?: boolean
  badge?: number
  icon?: string
}

interface AppShellProps {
  user: AuthUserDto
  role: UserRole
  navItems: NavItem[]
  onLogout: () => void
}

function NavLinkItem({item, compact = false}: {item: NavItem; compact?: boolean}) {
  return (
    <NavLink
      to={item.to}
      end={item.end}
      className={({isActive}) =>
        [
          'flex items-center gap-2 rounded-2xl font-medium transition',
          compact
            ? 'flex-col gap-0.5 px-2 py-2 text-[10px]'
            : 'px-4 py-2.5 text-sm',
          isActive
            ? 'gradient-brand text-white shadow-md shadow-brand-500/20'
            : 'text-stone-600 hover:bg-brand-50 hover:text-brand-700',
        ].join(' ')
      }
    >
      {item.icon != null && (
        <span className={compact ? 'text-base' : 'text-sm'} aria-hidden>
          {item.icon}
        </span>
      )}
      <span className={compact ? 'leading-tight' : ''}>{item.label}</span>
      {item.badge != null && item.badge > 0 ? <NavBadge count={item.badge} /> : null}
    </NavLink>
  )
}

export function AppShell({user, role, navItems, onLogout}: AppShellProps) {
  const {t} = useTranslation()
  const displayName = user.profileName ?? user.email

  return (
    <div className="min-h-screen pb-[4.5rem] md:pb-0">
      <div className="mx-auto flex w-full max-w-7xl md:gap-8 md:px-6 lg:px-8">
        <aside className="sticky top-0 hidden h-screen w-56 shrink-0 py-6 md:flex md:flex-col">
          <div className="flex h-full min-h-0 flex-col rounded-[var(--radius-card)] border border-brand-100/60 bg-white card-shadow">
            <div className="shrink-0 border-b border-brand-100/60 px-5 py-5">
              <BrandTitle size="md" />
              <p className="mt-2 text-xs font-medium text-brand-600">{displayRoleLabel(role, t)}</p>
            </div>

            <nav className="flex min-h-0 flex-1 flex-col gap-1 overflow-y-auto px-3 py-4">
              {navItems.map((item) => (
                <NavLinkItem key={item.to} item={item} />
              ))}
            </nav>

            <div className="shrink-0 border-t border-brand-100/60 p-4">
              <div className="flex items-center gap-3">
                <Avatar name={displayName} size="sm" />
                <div className="min-w-0 flex-1">
                  {user.profileName != null && user.profileName !== '' && (
                    <p className="truncate text-sm font-medium text-stone-900">{user.profileName}</p>
                  )}
                  <p className="truncate text-xs text-stone-500">{user.email}</p>
                </div>
              </div>
              <button
                type="button"
                onClick={onLogout}
                className="mt-3 w-full rounded-full border border-stone-200 px-3 py-1.5 text-sm text-stone-600 transition hover:bg-stone-50"
              >
                {t('appShell.logout')}
              </button>
            </div>
          </div>
        </aside>

        <main className="min-w-0 flex-1 px-4 py-5 sm:px-6 md:py-6 md:pl-0">
          <div className="mx-auto w-full max-w-5xl">
            <Outlet />
          </div>
        </main>
      </div>

      <nav className="fixed inset-x-0 bottom-0 z-50 flex justify-around border-t border-brand-100/60 bg-white/95 px-1 py-2 backdrop-blur-lg md:hidden">
        {navItems.map((item) => (
          <NavLinkItem key={item.to} item={item} compact />
        ))}
      </nav>
    </div>
  )
}
