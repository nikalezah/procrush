import {useTranslation} from 'react-i18next'

export function Spinner({size = 'md', className = ''}: {size?: 'sm' | 'md' | 'lg'; className?: string}) {
  const {t} = useTranslation()
  const sizeClasses = {
    sm: 'h-5 w-5 border-2',
    md: 'h-8 w-8 border-2',
    lg: 'h-10 w-10 border-4',
  } as const

  return (
    <div
      className={`animate-spin rounded-full border-brand-200 border-t-brand-600 ${sizeClasses[size]} ${className}`}
      role="status"
      aria-label={t('common.loading')}
    />
  )
}
