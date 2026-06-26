import {AlphaBadge} from './AlphaBadge'

interface BrandTitleProps {
  size?: 'sm' | 'md' | 'lg'
}

export function BrandTitle({size = 'md'}: BrandTitleProps) {
  const titleClass =
    size === 'lg'
      ? 'text-4xl font-bold tracking-tight'
      : size === 'sm'
        ? 'text-base font-bold tracking-tight'
        : 'text-xl font-bold tracking-tight'

  return (
    <div className="flex items-center gap-2">
      <span className={`gradient-text ${titleClass}`}>ProCrush</span>
      {size !== 'sm' && <AlphaBadge />}
    </div>
  )
}
