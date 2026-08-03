export type RecommendationsUpdatedEventDto = {
  scope: 'seeker' | 'job' | string
  id: number
  computedAt: string
}

function parseRecommendationsEvent(data: string): RecommendationsUpdatedEventDto | null {
  try {
    return JSON.parse(data) as RecommendationsUpdatedEventDto
  } catch {
    return null
  }
}

function subscribeRecommendationsEvents(
  url: string,
  onEvent: (event: RecommendationsUpdatedEventDto) => void,
  onError?: () => void,
): () => void {
  const eventSource = new EventSource(url)
  eventSource.addEventListener('recommendations-updated', (event) => {
    const messageEvent = event as MessageEvent<string>
    const payload = parseRecommendationsEvent(messageEvent.data)
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

export function subscribeSeekerRecommendationsUpdated(
  onEvent: (event: RecommendationsUpdatedEventDto) => void,
  onError?: () => void,
): () => void {
  return subscribeRecommendationsEvents('/api/seeker/recommendations/events', onEvent, onError)
}

export function subscribeEmployerRecommendationsUpdated(
  onEvent: (event: RecommendationsUpdatedEventDto) => void,
  onError?: () => void,
): () => void {
  return subscribeRecommendationsEvents('/api/employer/recommendations/events', onEvent, onError)
}
