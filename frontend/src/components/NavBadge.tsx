interface NavBadgeProps {
  count: number
}

export function NavBadge({count}: NavBadgeProps) {
  if (count <= 0) return null

  const label = count > 99 ? '99+' : String(count)

  return (
    <span className="ml-1.5 inline-flex min-w-5 items-center justify-center rounded-full bg-amber-500 px-1.5 py-0.5 text-xs font-semibold text-white">
      {label}
    </span>
  )
}
