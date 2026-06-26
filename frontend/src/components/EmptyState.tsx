interface EmptyStateProps {
  title: string
  description?: string
  action?: React.ReactNode
  icon?: string
}

export function EmptyState({title, description, action, icon = '💫'}: EmptyStateProps) {
  return (
    <div className="flex flex-col items-center rounded-2xl border border-dashed border-brand-200 bg-brand-50/50 px-6 py-12 text-center">
      <span className="text-4xl" aria-hidden>
        {icon}
      </span>
      <h3 className="mt-4 text-base font-semibold text-stone-800">{title}</h3>
      {description != null && <p className="mt-2 max-w-sm text-sm text-stone-500">{description}</p>}
      {action != null && <div className="mt-5">{action}</div>}
    </div>
  )
}
