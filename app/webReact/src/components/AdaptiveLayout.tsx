import type { ReactNode } from 'react'

interface AdaptiveLayoutProps {
  children: ReactNode
}

export function AdaptiveLayout({ children }: AdaptiveLayoutProps) {
  return (
    <div className="flex min-h-screen justify-center">
      <div className="w-full max-w-md px-5 py-6">{children}</div>
    </div>
  )
}
