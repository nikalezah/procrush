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
  return fetch('/api/seeker/match-interests/count').then((response) => {
    if (!response.ok) {
      throw new Error('Не удалось загрузить счётчик откликов')
    }
    return response.json() as Promise<MatchInterestCountDto>
  })
}

export function subscribeSeekerMatchInterestEvents(
  onEvent: (event: MatchInterestEventDto) => void,
  onError?: () => void,
): () => void {
  return subscribeMatchInterestEvents('/api/seeker/match-interests/events', onEvent, onError)
}

export function fetchEmployerMatchInterestCount(): Promise<MatchInterestCountDto> {
  return fetch('/api/employer/match-interests/count').then((response) => {
    if (!response.ok) {
      throw new Error('Не удалось загрузить счётчик откликов')
    }
    return response.json() as Promise<MatchInterestCountDto>
  })
}

export function subscribeEmployerMatchInterestEvents(
  onEvent: (event: MatchInterestEventDto) => void,
  onError?: () => void,
): () => void {
  return subscribeMatchInterestEvents('/api/employer/match-interests/events', onEvent, onError)
}
