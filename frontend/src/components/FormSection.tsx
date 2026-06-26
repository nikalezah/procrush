import type {ReactNode} from 'react'

interface FormSectionProps {
  title: string
  description?: string
  children: ReactNode
}

export function FormSection({title, description, children}: FormSectionProps) {
  return (
    <section className="rounded-[var(--radius-card)] border border-brand-100/60 bg-white p-5 card-shadow sm:p-6">
      <h2 className="text-lg font-semibold text-stone-900">{title}</h2>
      {description != null && <p className="mt-1 text-sm text-stone-500">{description}</p>}
      <div className="mt-4 flex flex-col gap-4">{children}</div>
    </section>
  )
}
