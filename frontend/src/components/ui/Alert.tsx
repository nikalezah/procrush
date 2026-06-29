import type {ReactNode} from 'react'

type AlertVariant = 'info' | 'success' | 'warning' | 'error' | 'danger'

interface AlertProps {
  variant?: AlertVariant
  title?: string
  children: ReactNode
  action?: ReactNode
}

const variantClasses: Record<AlertVariant, string> = {
  info: 'border-brand-200 bg-brand-50 text-brand-900',
  success: 'border-emerald-200 bg-emerald-50 text-emerald-900',
  warning: 'border-amber-200 bg-amber-50 text-amber-900',
  error: 'border-red-200 bg-red-50 text-red-800',
  danger: 'border-red-200 bg-white text-red-800',
}

export function Alert({variant = 'info', title, children, action}: AlertProps) {
  return (
    <div
      className={`flex flex-wrap items-center justify-between gap-3 rounded-2xl border px-4 py-3 text-sm ${variantClasses[variant]}`}
      role="alert"
    >
      <div>
        {title != null && <p className="font-semibold">{title}</p>}
        <div className={title != null ? 'mt-0.5 opacity-90' : ''}>{children}</div>
      </div>
      {action != null && <div className="shrink-0">{action}</div>}
    </div>
  )
}
