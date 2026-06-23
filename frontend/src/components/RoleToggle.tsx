import type { UserRole } from '../api/types'
import { displayRoleLabel } from '../lib/roleLabels'

interface RoleToggleProps {
  value: UserRole
  onChange: (role: UserRole) => void
  disabled?: boolean
}

const roles: UserRole[] = ['SEEKER', 'EMPLOYER']

export function RoleToggle({ value, onChange, disabled = false }: RoleToggleProps) {
  return (
    <div
      className="flex w-full rounded-lg border border-neutral-300 bg-neutral-100 p-1"
      role="radiogroup"
      aria-label="Тип аккаунта"
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
            className={`flex-1 rounded-md px-3 py-2 text-sm font-medium transition disabled:cursor-not-allowed disabled:opacity-60 ${
              selected
                ? 'bg-white text-neutral-900 shadow-sm'
                : 'text-neutral-600 hover:text-neutral-900'
            }`}
          >
            {displayRoleLabel(role)}
          </button>
        )
      })}
    </div>
  )
}
