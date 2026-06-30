import {useTranslation} from 'react-i18next'
import type {UserRole} from '../api/types'
import {displayRoleLabel} from '../lib/roleLabels'

interface RoleToggleProps {
  value: UserRole
  onChange: (role: UserRole) => void
  disabled?: boolean
}

const roles: UserRole[] = ['SEEKER', 'EMPLOYER']

const roleIcons: Record<UserRole, string> = {
  SEEKER: '🔍',
  EMPLOYER: '🏢',
}

export function RoleToggle({value, onChange, disabled = false}: RoleToggleProps) {
  const {t} = useTranslation()

  return (
    <div
      className="flex w-full rounded-2xl border border-border-subtle bg-surface-muted/50 p-1"
      role="radiogroup"
      aria-label={t('auth.roleToggle.ariaLabel')}
    >
      {roles.map((role) => {
        const selected = value === role
        return (
          <button
            key={role}
            type="button"
            role="radio"
            aria-checked={selected}
            disabled={disabled}
            onClick={() => onChange(role)}
            className={[
              'flex flex-1 flex-col items-center gap-1 rounded-xl px-3 py-3 text-sm font-medium transition sm:flex-row sm:justify-center',
              'disabled:cursor-not-allowed disabled:opacity-60',
              selected
                ? 'gradient-brand text-white shadow-md shadow-brand-500/20'
                : 'text-muted hover:bg-surface/60 hover:text-foreground',
            ].join(' ')}
          >
            <span aria-hidden>{roleIcons[role]}</span>
            {displayRoleLabel(role, t)}
          </button>
        )
      })}
    </div>
  )
}
