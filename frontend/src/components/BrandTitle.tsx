import {AlphaBadge} from './AlphaBadge'

interface BrandTitleProps {
  size?: 'md' | 'lg'
}

export function BrandTitle({ size = 'md' }: BrandTitleProps) {
  const titleClass =
    size === 'lg'
      ? 'text-3xl font-semibold tracking-tight'
      : 'text-lg font-semibold tracking-tight'

  return (
    <div className="flex items-center gap-2">
      <span className={titleClass}>ProCrush</span>
      <AlphaBadge />
    </div>
  )
}
