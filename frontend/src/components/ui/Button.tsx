import type {ButtonHTMLAttributes, ReactNode} from 'react'

type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'danger' | 'like'
type ButtonSize = 'sm' | 'md' | 'lg'

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant
  size?: ButtonSize
  fullWidth?: boolean
  children: ReactNode
}

const variantClasses: Record<ButtonVariant, string> = {
  primary:
    'gradient-brand text-white shadow-md shadow-brand-500/25 hover:brightness-110 active:scale-[0.98]',
  secondary:
    'border border-brand-200 bg-white text-brand-700 hover:bg-brand-50 active:scale-[0.98]',
  ghost: 'text-stone-600 hover:bg-stone-100/80 active:scale-[0.98]',
  danger: 'bg-red-50 text-red-700 hover:bg-red-100 active:scale-[0.98]',
  like: 'gradient-brand text-white shadow-lg shadow-brand-500/30 hover:brightness-110 active:scale-95 animate-heart-beat',
}

const sizeClasses: Record<ButtonSize, string> = {
  sm: 'px-3 py-1.5 text-sm',
  md: 'px-5 py-2.5 text-sm font-medium',
  lg: 'px-6 py-3 text-base font-semibold',
}

export function Button({
  variant = 'primary',
  size = 'md',
  fullWidth = false,
  className = '',
  disabled,
  type = 'button',
  children,
  ...props
}: ButtonProps) {
  return (
    <button
      type={type}
      disabled={disabled}
      className={[
        'inline-flex items-center justify-center gap-2 rounded-full transition-all',
        'disabled:cursor-not-allowed disabled:opacity-50 disabled:shadow-none disabled:active:scale-100',
        variantClasses[variant],
        sizeClasses[size],
        fullWidth ? 'w-full' : '',
        className,
      ]
        .filter(Boolean)
        .join(' ')}
      {...props}
    >
      {children}
    </button>
  )
}
