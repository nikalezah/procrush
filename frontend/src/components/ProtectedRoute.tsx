import { Navigate } from 'react-router-dom'
import { LoadingSpinner } from './LoadingSpinner'
import type { AuthState, UserRole } from '../api/types'

interface ProtectedRouteProps {
  state: AuthState
  requiredRole: UserRole
  children: React.ReactNode
}

export function ProtectedRoute({ state, requiredRole, children }: ProtectedRouteProps) {
  if (state.kind === 'loading') return <LoadingSpinner />
  if (state.kind === 'unauthenticated') return <Navigate to="/" replace />
  if (state.kind === 'needsRegistration') return <Navigate to="/auth/role" replace />
  if (state.user.role !== requiredRole) {
    const redirect = state.user.role === 'EMPLOYER' ? '/employer' : '/seeker'
    return <Navigate to={redirect} replace />
  }
  return <>{children}</>
}
