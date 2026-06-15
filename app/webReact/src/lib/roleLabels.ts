import type { UserRole } from '../api/types'

const labels: Record<UserRole, string> = {
  SEEKER: 'Соискатель',
  EMPLOYER: 'Работодатель',
}

export function displayRoleLabel(role: UserRole | null | undefined): string {
  if (role == null) return 'неизвестно'
  return labels[role]
}
