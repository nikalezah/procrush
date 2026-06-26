interface AvatarProps {
  name?: string | null
  size?: 'sm' | 'md' | 'lg'
  className?: string
}

const sizeClasses = {
  sm: 'h-8 w-8 text-xs',
  md: 'h-10 w-10 text-sm',
  lg: 'h-14 w-14 text-lg',
}

function initials(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean)
  if (parts.length === 0) return '?'
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase()
  return (parts[0][0] + parts[1][0]).toUpperCase()
}

export function Avatar({name, size = 'md', className = ''}: AvatarProps) {
  const label = name != null && name !== '' ? initials(name) : '?'

  return (
    <div
      className={[
        'flex shrink-0 items-center justify-center rounded-full font-semibold text-white gradient-brand',
        sizeClasses[size],
        className,
      ].join(' ')}
      aria-hidden
    >
      {label}
    </div>
  )
}
