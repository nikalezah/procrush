import {createContext, type ReactNode, useCallback, useContext, useEffect, useMemo, useState,} from 'react'
import {useLocation} from 'react-router-dom'
import {fetchPersonalityPreview, subscribePersonalityStatusEvents} from '../api/seekerApi'
import type {PersonalityProfileStatus} from '../api/types'

interface PersonalityReadyEventsContextValue {
  badgeCount: number
}

const PersonalityReadyEventsContext = createContext<PersonalityReadyEventsContextValue | null>(null)

const ACK_STORAGE_KEY = 'procrush:personality-result-ack'

function readAck(): boolean {
  return sessionStorage.getItem(ACK_STORAGE_KEY) === 'true'
}

function writeAck(acked: boolean): void {
  if (acked) {
    sessionStorage.setItem(ACK_STORAGE_KEY, 'true')
  } else {
    sessionStorage.removeItem(ACK_STORAGE_KEY)
  }
}

function isViewPage(pathname: string): boolean {
  return pathname === '/seeker/personality'
}

function isActionableStatus(status: PersonalityProfileStatus): boolean {
  return status === 'READY' || status === 'FAILED'
}

function usePersonalityReadyEventsState(): PersonalityReadyEventsContextValue {
  const location = useLocation()
  const onViewPage = isViewPage(location.pathname)
  const [badgeCount, setBadgeCount] = useState(0)
  const [shouldSubscribe, setShouldSubscribe] = useState(false)
  const [subscribeGeneration, setSubscribeGeneration] = useState(0)

  const syncBadge = useCallback(async (viewing: boolean) => {
    try {
      const preview = await fetchPersonalityPreview()
      const {status} = preview
      const actionable = isActionableStatus(status)

      if (status === 'PROCESSING') {
        writeAck(false)
      }

      if (viewing) {
        if (actionable) writeAck(true)
        setBadgeCount(0)
        setShouldSubscribe(status === 'PROCESSING')
        return
      }

      setBadgeCount(actionable && !readAck() ? 1 : 0)
      setShouldSubscribe(status === 'PROCESSING')
    } catch {
      // ignore preview load errors; SSE reconnect will retry
    }
  }, [])

  useEffect(() => {
    void syncBadge(onViewPage)
  }, [onViewPage, syncBadge])

  useEffect(() => {
    if (!shouldSubscribe) return

    const unsubscribe = subscribePersonalityStatusEvents(
      (status) => {
        if (isActionableStatus(status)) {
          void syncBadge(onViewPage)
        }
      },
      () => {
        void syncBadge(onViewPage).finally(() => {
          setSubscribeGeneration((current) => current + 1)
        })
      },
    )

    return unsubscribe
  }, [shouldSubscribe, onViewPage, syncBadge, subscribeGeneration])

  return useMemo(
    () => ({
      badgeCount: onViewPage ? 0 : badgeCount,
    }),
    [badgeCount, onViewPage],
  )
}

export function PersonalityReadyEventsProvider({children}: {children: ReactNode}) {
  const value = usePersonalityReadyEventsState()
  return (
    <PersonalityReadyEventsContext.Provider value={value}>{children}</PersonalityReadyEventsContext.Provider>
  )
}

export function usePersonalityReadyEvents(): PersonalityReadyEventsContextValue {
  const context = useContext(PersonalityReadyEventsContext)
  if (context == null) {
    throw new Error('usePersonalityReadyEvents must be used within PersonalityReadyEventsProvider')
  }
  return context
}
