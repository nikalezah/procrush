import {NavLink, Outlet} from 'react-router-dom'
import type {AuthUserDto, UserRole} from '../api/types'
import {displayRoleLabel} from '../lib/roleLabels'
import {BrandTitle} from './BrandTitle'
import {NavBadge} from './NavBadge'

export interface NavItem {
  to: string
  label: string
  end?: boolean
  badge?: number
}

interface AppShellProps {
  user: AuthUserDto
  role: UserRole
  navItems: NavItem[]
  onLogout: () => void
}

export function AppShell({ user, role, navItems, onLogout }: AppShellProps) {
  return (
    <div className="flex min-h-screen flex-col bg-neutral-50 md:flex-row">
      <aside className="border-b border-neutral-200 bg-white md:w-60 md:border-b-0 md:border-r">
        <div className="px-5 py-5">
          <BrandTitle size="md" />
          <p className="mt-1 text-xs text-neutral-500">{displayRoleLabel(role)}</p>
        </div>
        <nav className="flex gap-1 overflow-x-auto px-3 pb-3 md:flex-col md:overflow-visible md:px-3 md:pb-6">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) =>
                `whitespace-nowrap rounded-lg px-3 py-2 text-sm font-medium transition ${
                  isActive
                    ? 'bg-neutral-900 text-white'
                    : 'text-neutral-700 hover:bg-neutral-100'
                }`
              }
            >
              {item.label}
              {item.badge != null && item.badge > 0 ? <NavBadge count={item.badge} /> : null}
            </NavLink>
          ))}
        </nav>
      </aside>
      <div className="flex min-w-0 flex-1 flex-col">
        <header className="flex items-center justify-between border-b border-neutral-200 bg-white px-5 py-3">
          <div className="min-w-0 truncate">
            {user.profileName != null && user.profileName !== '' && (
              <p className="truncate text-sm font-medium text-neutral-900">{user.profileName}</p>
            )}
            <p className="truncate text-sm text-neutral-600">{user.email}</p>
          </div>
          <button
            type="button"
            onClick={onLogout}
            className="rounded-lg border border-neutral-300 px-3 py-1.5 text-sm hover:bg-neutral-50"
          >
            Выйти
          </button>
        </header>
        <main className="mx-auto w-full max-w-5xl flex-1 px-5 py-6">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
