import {useTranslation} from 'react-i18next'

type CompanyNameProps = {
  name: string | null | undefined
  className?: string
}

export function CompanyName({name, className}: CompanyNameProps) {
  const {t} = useTranslation()
  const label = name?.trim() ? name : t('common.companyNotSpecified')
  if (className != null) {
    return <span className={className}>{label}</span>
  }
  return <>{label}</>
}

export function companyNameLabel(
  name: string | null | undefined,
  t: (key: 'common.companyNotSpecified') => string,
): string {
  return name?.trim() ? name : t('common.companyNotSpecified')
}
