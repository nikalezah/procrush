import type {ReactNode} from 'react'

interface CardProps {
  children: ReactNode
  className?: string
  highlighted?: boolean
  padding?: 'sm' | 'md' | 'lg'
}

const paddingClasses = {
  sm: 'p-4',
  md: 'p-5',
  lg: 'p-6',
}

export function Card({children, className = '', highlighted = false, padding = 'md'}: CardProps) {
  return (
    <div
      className={[
        'rounded-[var(--radius-card)] border border-brand-100/60 bg-white card-shadow',
        paddingClasses[padding],
        highlighted ? 'ring-2 ring-accent-300 animate-pulse-match' : '',
        className,
      ]
        .filter(Boolean)
        .join(' ')}
    >
      {children}
    </div>
  )
}

interface StatCardProps {
  label: string
  value: string | number
  action?: ReactNode
  icon?: ReactNode
}

export function StatCard({label, value, action, icon}: StatCardProps) {
  return (
    <Card className="flex flex-col gap-2">
      <div className="flex items-center justify-between">
        <p className="text-sm text-stone-500">{label}</p>
        {icon != null && <span className="text-brand-400">{icon}</span>}
      </div>
      <p className="text-3xl font-bold text-stone-900">{value}</p>
      {action != null && <div className="mt-1">{action}</div>}
    </Card>
  )
}
