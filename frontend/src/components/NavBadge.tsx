interface NavBadgeProps {
  count: number
}

export function NavBadge({count}: NavBadgeProps) {
  if (count <= 0) return null

  const label = count > 99 ? '99+' : String(count)

  return (
    <span className="inline-flex min-w-5 items-center justify-center rounded-full bg-surface px-1.5 py-0.5 text-[10px] font-bold text-brand-600 shadow-sm md:ml-1">
      {label}
    </span>
  )
}
