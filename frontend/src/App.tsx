import {BrowserRouter, Navigate, Route, Routes, useLocation} from 'react-router-dom'
import {useTranslation} from 'react-i18next'
import type {NavItem} from './components/AppShell'
import {AppShell} from './components/AppShell'
import {LoadingSpinner} from './components/LoadingSpinner'
import {ProtectedRoute} from './components/ProtectedRoute'
import {useDocumentTitle} from './hooks/useDocumentTitle'
import {AuthProvider, useAuth} from './hooks/useAuth'
import {EmployerCandidatesPage} from './pages/employer/EmployerCandidatesPage'
import {EmployerDashboardPage} from './pages/employer/EmployerDashboardPage'
import {EmployerProfilesPage} from './pages/employer/EmployerProfilesPage'
import {LoginPage} from './pages/LoginPage'
import {RoleSelectionPage} from './pages/RoleSelectionPage'
import {SeekerDashboardPage} from './pages/seeker/SeekerDashboardPage'
import {SeekerPersonalityPage} from './pages/seeker/SeekerPersonalityPage'
import {SeekerTestTakePage} from './pages/seeker/SeekerTestTakePage'
import {SeekerTestsListPage} from './pages/seeker/SeekerTestsListPage'
import {SeekerPositionsPage} from './pages/seeker/SeekerPositionsPage'
import {SeekerProfilePage} from './pages/seeker/SeekerProfilePage'
import {SettingsPage} from './pages/shared/SettingsPage'
import {
  EmployerMatchInterestEventsProvider,
  SeekerMatchInterestEventsProvider,
  useMatchInterestEvents,
} from './hooks/useMatchInterestEvents'
import {PersonalityReadyEventsProvider, usePersonalityReadyEvents,} from './hooks/usePersonalityReadyEvents'
import type {AuthUserDto} from './api/types'

function AuthFlow() {
  const { state, errorMessage, isBusy, signInDev, completeRegistration } = useAuth()
  const location = useLocation()
  const isRolePath = location.pathname.endsWith('/auth/role')

  if (state.kind === 'authenticated') {
    const home = state.user.role === 'EMPLOYER' ? '/employer' : '/seeker'
    if (location.pathname === '/' || isRolePath) {
      return <Navigate to={home} replace />
    }
  }

  if (isRolePath) {
    switch (state.kind) {
      case 'needsRegistration':
        return (
          <RoleSelectionPage
            user={state.user}
            isBusy={isBusy}
            errorMessage={errorMessage}
            onCompleteRegistration={(request) => void completeRegistration(request)}
          />
        )
      case 'authenticated':
        return <Navigate to={state.user.role === 'EMPLOYER' ? '/employer' : '/seeker'} replace />
      case 'loading':
      case 'unauthenticated':
        return <LoadingSpinner />
    }
  }

  switch (state.kind) {
    case 'loading':
      return <LoadingSpinner />
    case 'unauthenticated':
      return (
        <LoginPage
          isBusy={isBusy}
          errorMessage={errorMessage}
          onSignIn={(email) => void signInDev(email)}
        />
      )
    case 'needsRegistration':
      return (
        <RoleSelectionPage
          user={state.user}
          isBusy={isBusy}
          errorMessage={errorMessage}
          onCompleteRegistration={(request) => void completeRegistration(request)}
        />
      )
    case 'authenticated':
      return <Navigate to={state.user.role === 'EMPLOYER' ? '/employer' : '/seeker'} replace />
  }
}

function SeekerAppContent({
  user,
  onLogout,
}: {
  user: AuthUserDto
  onLogout: () => void
}) {
  const {t} = useTranslation()
  const { badgeCount: matchBadgeCount } = useMatchInterestEvents()
  const { badgeCount: personalityBadgeCount } = usePersonalityReadyEvents()
  const seekerNav: NavItem[] = [
    { to: '/seeker', label: t('nav.seeker.home'), end: true, icon: '🏠' },
    { to: '/seeker/positions', label: t('nav.seeker.positions'), icon: '💕' },
    { to: '/seeker/personality', label: t('nav.seeker.personality'), icon: '✨' },
    { to: '/seeker/profile', label: t('nav.seeker.profile'), icon: '👤' },
    { to: '/seeker/settings', label: t('nav.seeker.account'), icon: '🪪' },
  ]
  const navItems: NavItem[] = seekerNav.map((item) => {
    if (item.to === '/seeker/positions') return { ...item, badge: matchBadgeCount }
    if (item.to === '/seeker/personality') return { ...item, badge: personalityBadgeCount }
    return item
  })
  return <AppShell user={user} role="SEEKER" navItems={navItems} onLogout={onLogout} />
}

function EmployerAppContent({
  user,
  onLogout,
}: {
  user: AuthUserDto
  onLogout: () => void
}) {
  const {t} = useTranslation()
  const { badgeCount } = useMatchInterestEvents()
  const employerNav: NavItem[] = [
    { to: '/employer', label: t('nav.employer.home'), end: true, icon: '🏠' },
    { to: '/employer/profiles', label: t('nav.employer.profiles'), icon: '💼' },
    { to: '/employer/settings', label: t('nav.employer.account'), icon: '🪪' },
  ]
  const navItems: NavItem[] = employerNav.map((item) =>
    item.to === '/employer/profiles' ? { ...item, badge: badgeCount } : item,
  )
  return <AppShell user={user} role="EMPLOYER" navItems={navItems} onLogout={onLogout} />
}

function SeekerLayout() {
  const { state, signOut } = useAuth()
  if (state.kind === 'loading') return <LoadingSpinner />
  return (
    <ProtectedRoute state={state} requiredRole="SEEKER">
      {state.kind === 'authenticated' ? (
        <SeekerMatchInterestEventsProvider>
          <PersonalityReadyEventsProvider>
            <SeekerAppContent user={state.user} onLogout={() => void signOut()} />
          </PersonalityReadyEventsProvider>
        </SeekerMatchInterestEventsProvider>
      ) : null}
    </ProtectedRoute>
  )
}

function EmployerLayout() {
  const { state, signOut } = useAuth()
  if (state.kind === 'loading') return <LoadingSpinner />
  return (
    <ProtectedRoute state={state} requiredRole="EMPLOYER">
      {state.kind === 'authenticated' ? (
        <EmployerMatchInterestEventsProvider>
          <EmployerAppContent user={state.user} onLogout={() => void signOut()} />
        </EmployerMatchInterestEventsProvider>
      ) : null}
    </ProtectedRoute>
  )
}

function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<AuthFlow />} />
      <Route path="/auth/role" element={<AuthFlow />} />
      <Route path="/seeker" element={<SeekerLayout />}>
        <Route index element={<SeekerDashboardPage />} />
        <Route path="personality" element={<SeekerPersonalityPage />} />
        <Route path="personality/tests" element={<SeekerTestsListPage />} />
        <Route path="personality/tests/:surveyId" element={<SeekerTestTakePage />} />
        <Route path="profile" element={<SeekerProfilePage />} />
        <Route path="positions" element={<SeekerPositionsPage />} />
        <Route path="settings" element={<SettingsPage />} />
      </Route>
      <Route path="/employer" element={<EmployerLayout />}>
        <Route index element={<EmployerDashboardPage />} />
        <Route path="company" element={<Navigate to="/employer/settings" replace />} />
        <Route path="profiles" element={<EmployerProfilesPage />} />
        <Route path="profiles/:id/candidates" element={<EmployerCandidatesPage />} />
        <Route path="settings" element={<SettingsPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

export default function App() {
  useDocumentTitle()
  return (
    <BrowserRouter>
      <AuthProvider>
        <AppRoutes />
      </AuthProvider>
    </BrowserRouter>
  )
}
