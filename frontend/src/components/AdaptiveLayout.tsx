import type {ReactNode} from 'react'
import {BrandTitle} from './BrandTitle'
import {ThemeToggle} from './ThemeToggle'

interface AdaptiveLayoutProps {
  children: ReactNode
}

export function AdaptiveLayout({children}: AdaptiveLayoutProps) {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center px-4 py-8">
      <div className="absolute right-4 top-4">
        <ThemeToggle variant="compact" />
      </div>
      <div className="w-full max-w-md">
        <div className="mb-8 flex justify-center">
          <BrandTitle size="lg" />
        </div>
        <div className="rounded-[var(--radius-card)] border border-border-subtle bg-surface/90 p-6 card-shadow-lg backdrop-blur-sm sm:p-8">
          {children}
        </div>
      </div>
    </div>
  )
}
