import type {InputHTMLAttributes, ReactNode} from 'react'

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string
  hint?: string
  error?: string
  trailing?: ReactNode
}

export function Input({label, hint, error, trailing, className = '', id, ...props}: InputProps) {
  const inputId = id ?? label.toLowerCase().replace(/\s+/g, '-')

  return (
    <label className="flex w-full flex-col gap-1.5" htmlFor={inputId}>
      <span className="text-sm font-medium text-stone-700">{label}</span>
      <div className="relative">
        <input
          id={inputId}
          className={[
            'w-full rounded-2xl border border-stone-200 bg-white px-4 py-2.5 text-base text-stone-900',
            'outline-none transition placeholder:text-stone-400',
            'focus:border-brand-300 focus:ring-2 focus:ring-brand-200',
            'disabled:cursor-not-allowed disabled:bg-stone-50 disabled:opacity-60',
            trailing ? 'pr-10' : '',
            error ? 'border-red-300 focus:border-red-400 focus:ring-red-100' : '',
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
      {hint != null && error == null && <span className="text-xs text-stone-500">{hint}</span>}
      {error != null && <span className="text-xs text-red-600">{error}</span>}
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
      <span className="text-sm font-medium text-stone-700">{label}</span>
      <textarea
        id={inputId}
        className={[
          'w-full rounded-2xl border border-stone-200 bg-white px-4 py-2.5 text-sm text-stone-900',
          'outline-none transition placeholder:text-stone-400',
          'focus:border-brand-300 focus:ring-2 focus:ring-brand-200',
          'disabled:cursor-not-allowed disabled:bg-stone-50 disabled:opacity-60',
          className,
        ]
          .filter(Boolean)
          .join(' ')}
        {...props}
      />
      {hint != null && <span className="text-xs text-stone-500">{hint}</span>}
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
      <span className="text-sm font-medium text-stone-700">{label}</span>
      <select
        id={inputId}
        className={[
          'w-full rounded-2xl border border-stone-200 bg-white px-4 py-2.5 text-sm text-stone-900',
          'outline-none transition focus:border-brand-300 focus:ring-2 focus:ring-brand-200',
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
