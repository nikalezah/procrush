const sizeClasses = {
  sm: 'h-5 w-5 border-2',
  md: 'h-8 w-8 border-2',
  lg: 'h-10 w-10 border-4',
} as const

interface SpinnerProps {
  size?: keyof typeof sizeClasses
  className?: string
}

export function Spinner({ size = 'md', className = '' }: SpinnerProps) {
  return (
    <div
      className={`animate-spin rounded-full border-neutral-300 border-t-neutral-900 ${sizeClasses[size]} ${className}`}
      role="status"
      aria-label="Загрузка"
    />
  )
}
