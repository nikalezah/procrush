interface EmptyStateProps {
  title: string
  description?: string
  action?: React.ReactNode
}

export function EmptyState({ title, description, action }: EmptyStateProps) {
  return (
    <div className="rounded-xl border border-dashed border-neutral-300 bg-neutral-50 px-6 py-10 text-center">
      <h3 className="text-base font-medium text-neutral-800">{title}</h3>
      {description != null && (
        <p className="mt-2 text-sm text-neutral-600">{description}</p>
      )}
      {action != null && <div className="mt-4">{action}</div>}
    </div>
  )
}
