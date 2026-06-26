import type {ReactNode} from 'react'
import {Link} from 'react-router-dom'

interface PageHeaderProps {
  title: string
  subtitle?: string
  backTo?: string
  backLabel?: string
  action?: ReactNode
}

export function PageHeader({title, subtitle, backTo, backLabel, action}: PageHeaderProps) {
  return (
    <header className="flex flex-col gap-3">
      {backTo != null && (
        <Link
          to={backTo}
          className="inline-flex w-fit items-center gap-1 text-sm font-medium text-brand-600 hover:text-brand-700"
        >
          <span aria-hidden>←</span> {backLabel ?? 'Назад'}
        </Link>
      )}
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-stone-900 sm:text-3xl">{title}</h1>
          {subtitle != null && <p className="mt-1 text-sm text-stone-500 sm:text-base">{subtitle}</p>}
        </div>
        {action != null && <div className="shrink-0">{action}</div>}
      </div>
    </header>
  )
}
