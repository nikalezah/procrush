import {apiFetch} from './client'
import type {MatchInterestCountDto, MatchInterestEventDto} from './types'

function parseMatchInterestEvent(data: string): MatchInterestEventDto | null {
  try {
    return JSON.parse(data) as MatchInterestEventDto
  } catch {
    return null
  }
}

function subscribeMatchInterestEvents(
  url: string,
  onEvent: (event: MatchInterestEventDto) => void,
  onError?: () => void,
): () => void {
  const eventSource = new EventSource(url)
  eventSource.addEventListener('match-interest', (event) => {
    const messageEvent = event as MessageEvent<string>
    const payload = parseMatchInterestEvent(messageEvent.data)
    if (payload != null) {
      onEvent(payload)
    }
  })
  eventSource.onerror = () => {
    eventSource.close()
    onError?.()
  }
  return () => eventSource.close()
}

export function fetchSeekerMatchInterestCount(): Promise<MatchInterestCountDto> {
  return apiFetch('/api/seeker/match-interests/count')
}

export function subscribeSeekerMatchInterestEvents(
  onEvent: (event: MatchInterestEventDto) => void,
  onError?: () => void,
): () => void {
  return subscribeMatchInterestEvents('/api/seeker/match-interests/events', onEvent, onError)
}

export function fetchEmployerMatchInterestCount(): Promise<MatchInterestCountDto> {
  return apiFetch('/api/employer/match-interests/count')
}

export function subscribeEmployerMatchInterestEvents(
  onEvent: (event: MatchInterestEventDto) => void,
  onError?: () => void,
): () => void {
  return subscribeMatchInterestEvents('/api/employer/match-interests/events', onEvent, onError)
}
