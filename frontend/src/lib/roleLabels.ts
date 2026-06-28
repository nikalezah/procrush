import type {TFunction} from 'i18next'
import type {UserRole} from '../api/types'
import i18n from '../i18n/config'

export function displayRoleLabel(role: UserRole | null | undefined, t?: TFunction): string {
  const translate = t ?? ((key: string) => i18n.t(key))
  if (role == null) return translate('role.unknown')
  return role === 'SEEKER' ? translate('role.seeker') : translate('role.employer')
}
