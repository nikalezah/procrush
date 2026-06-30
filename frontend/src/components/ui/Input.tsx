import type {InputHTMLAttributes, ReactNode} from 'react'

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string
  hint?: string
  error?: string
  trailing?: ReactNode
}

const fieldBaseClasses = [
  'w-full rounded-2xl border border-border bg-surface px-4 py-2.5 text-base text-foreground',
  'outline-none transition placeholder:text-muted',
  'focus:border-brand-300 focus:ring-2 focus:ring-brand-200 dark:focus:ring-brand-900',
  'disabled:cursor-not-allowed disabled:bg-surface-muted disabled:opacity-60',
]

const fieldErrorClasses = 'border-red-300 focus:border-red-400 focus:ring-red-100 dark:border-red-800 dark:focus:ring-red-950'

export function Input({label, hint, error, trailing, className = '', id, ...props}: InputProps) {
  const inputId = id ?? label.toLowerCase().replace(/\s+/g, '-')

  return (
    <label className="flex w-full flex-col gap-1.5" htmlFor={inputId}>
      <span className="text-sm font-medium text-foreground">{label}</span>
      <div className="relative">
        <input
          id={inputId}
          className={[
            ...fieldBaseClasses,
            trailing ? 'pr-10' : '',
            error ? fieldErrorClasses : '',
            className,
          ]
            .filter(Boolean)
            .join(' ')}
          {...props}
        />
        {trailing != null && (
          <div className="pointer-events-none absolute inset-y-0 right-3 flex items-center">
            {trailing}
          </div>
        )}
      </div>
      {hint != null && error == null && <span className="text-xs text-muted">{hint}</span>}
      {error != null && <span className="text-xs text-red-600 dark:text-red-400">{error}</span>}
    </label>
  )
}

interface TextAreaProps extends React.TextareaHTMLAttributes<HTMLTextAreaElement> {
  label: string
  hint?: string
}

export function TextArea({label, hint, className = '', id, ...props}: TextAreaProps) {
  const inputId = id ?? label.toLowerCase().replace(/\s+/g, '-')

  return (
    <label className="flex w-full flex-col gap-1.5" htmlFor={inputId}>
      <span className="text-sm font-medium text-foreground">{label}</span>
      <textarea
        id={inputId}
        className={[
          'w-full rounded-2xl border border-border bg-surface px-4 py-2.5 text-sm text-foreground',
          'outline-none transition placeholder:text-muted',
          'focus:border-brand-300 focus:ring-2 focus:ring-brand-200 dark:focus:ring-brand-900',
          'disabled:cursor-not-allowed disabled:bg-surface-muted disabled:opacity-60',
          className,
        ]
          .filter(Boolean)
          .join(' ')}
        {...props}
      />
      {hint != null && <span className="text-xs text-muted">{hint}</span>}
    </label>
  )
}

interface SelectProps extends React.SelectHTMLAttributes<HTMLSelectElement> {
  label: string
}

export function Select({label, className = '', id, children, ...props}: SelectProps) {
  const inputId = id ?? label.toLowerCase().replace(/\s+/g, '-')

  return (
    <label className="flex w-full flex-col gap-1.5" htmlFor={inputId}>
      <span className="text-sm font-medium text-foreground">{label}</span>
      <select
        id={inputId}
        className={[
          'w-full rounded-2xl border border-border bg-surface px-4 py-2.5 text-sm text-foreground',
          'outline-none transition focus:border-brand-300 focus:ring-2 focus:ring-brand-200 dark:focus:ring-brand-900',
          className,
        ]
          .filter(Boolean)
          .join(' ')}
        {...props}
      >
        {children}
      </select>
    </label>
  )
}
