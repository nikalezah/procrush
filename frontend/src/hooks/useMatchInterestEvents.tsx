import {
    createContext,
    type ReactNode,
    useCallback,
    useContext,
    useEffect,
    useLayoutEffect,
    useMemo,
    useState,
} from 'react'
import {useLocation} from 'react-router-dom'
import {
    fetchEmployerMatchInterestCount,
    fetchSeekerMatchInterestCount,
    subscribeEmployerMatchInterestEvents,
    subscribeSeekerMatchInterestEvents,
} from '../api/matchInterestApi'
import type {MatchInterestEventDto} from '../api/types'

type MatchInterestRole = 'seeker' | 'employer'

interface MatchInterestEventsContextValue {
  badgeCount: number
  lastEvent: MatchInterestEventDto | null
  lastEventId: number
}

const MatchInterestEventsContext = createContext<MatchInterestEventsContextValue | null>(null)

function ackStorageKey(role: MatchInterestRole): string {
  return `procrush:match-interest-ack:${role}`
}

function readAckBaseline(role: MatchInterestRole): number {
  const raw = sessionStorage.getItem(ackStorageKey(role))
  if (raw == null) return 0
  const parsed = Number(raw)
  return Number.isFinite(parsed) ? parsed : 0
}

function writeAckBaseline(role: MatchInterestRole, count: number): void {
  sessionStorage.setItem(ackStorageKey(role), String(count))
}

function isViewPage(role: MatchInterestRole, pathname: string): boolean {
  if (role === 'seeker') {
    return pathname === '/seeker/positions'
  }
  return pathname === '/employer/profiles' || pathname.startsWith('/employer/profiles/')
}

function isActionableEvent(event: MatchInterestEventDto): boolean {
  return event.interestStatus === 'INCOMING' || event.interestStatus === 'MUTUAL'
}

function useMatchInterestEventsState(role: MatchInterestRole): MatchInterestEventsContextValue {
  const location = useLocation()
  const onViewPage = isViewPage(role, location.pathname)
  const [badgeCount, setBadgeCount] = useState(0)
  const [lastEvent, setLastEvent] = useState<MatchInterestEventDto | null>(null)
  const [lastEventId, setLastEventId] = useState(0)

  useLayoutEffect(() => {
    if (onViewPage) {
      setLastEvent(null)
    }
  }, [onViewPage, location.pathname])

  const syncBadge = useCallback(
    async (viewing: boolean) => {
      const fetchCount =
        role === 'seeker' ? fetchSeekerMatchInterestCount : fetchEmployerMatchInterestCount
      try {
        const {count} = await fetchCount()
        if (viewing) {
          writeAckBaseline(role, count)
          setBadgeCount(0)
          return
        }
        setBadgeCount(Math.max(0, count - readAckBaseline(role)))
      } catch {
        // ignore count load errors; SSE reconnect will retry
      }
    },
    [role],
  )

  useEffect(() => {
    void syncBadge(onViewPage)
  }, [onViewPage, syncBadge])

  useEffect(() => {
    const subscribeEvents =
      role === 'seeker' ? subscribeSeekerMatchInterestEvents : subscribeEmployerMatchInterestEvents

    const unsubscribe = subscribeEvents(
      (event) => {
        if (!isActionableEvent(event)) return
        setLastEventId((current) => current + 1)
        setLastEvent(event)
        void syncBadge(onViewPage)
      },
      () => {
        void syncBadge(onViewPage)
      },
    )

    return unsubscribe
  }, [role, onViewPage, syncBadge])

  return useMemo(
    () => ({
      badgeCount: onViewPage ? 0 : badgeCount,
      lastEvent,
      lastEventId,
    }),
    [badgeCount, lastEvent, lastEventId, onViewPage],
  )
}

export function SeekerMatchInterestEventsProvider({children}: {children: ReactNode}) {
  const value = useMatchInterestEventsState('seeker')
  return (
    <MatchInterestEventsContext.Provider value={value}>{children}</MatchInterestEventsContext.Provider>
  )
}

export function EmployerMatchInterestEventsProvider({children}: {children: ReactNode}) {
  const value = useMatchInterestEventsState('employer')
  return (
    <MatchInterestEventsContext.Provider value={value}>{children}</MatchInterestEventsContext.Provider>
  )
}

export function useMatchInterestEvents(): MatchInterestEventsContextValue {
  const context = useContext(MatchInterestEventsContext)
  if (context == null) {
    throw new Error('useMatchInterestEvents must be used within a match interest events provider')
  }
  return context
}
