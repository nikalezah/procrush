import {apiFetch} from './client'
import type {
    CompleteSurveyResponseDto,
    CreateSeekerEducationRequest,
    CreateSeekerExperienceRequest,
    JobRecommendationDto,
    PersonalityPreviewDto,
    PersonalityProfileStatus,
    SeekerDashboardDto,
    SeekerDesiredPositionsResponse,
    SeekerEducationDto,
    SeekerExperienceDto,
    SeekerInterestsResponseDto,
    SeekerPositionsOverviewDto,
    SeekerProfileDto,
    SeekerSkillsResponse,
    SurveyDetailDto,
    SurveyGroupsResponseDto,
    UpdateSeekerProfileRequest,
} from './types'

const jsonHeaders = { 'Content-Type': 'application/json' }

export function fetchSeekerDashboard(): Promise<SeekerDashboardDto> {
  return apiFetch('/api/seeker/dashboard')
}

export function fetchSeekerProfile(): Promise<SeekerProfileDto> {
  return apiFetch('/api/seeker/me')
}

export function updateSeekerProfile(body: UpdateSeekerProfileRequest): Promise<SeekerProfileDto> {
  return apiFetch('/api/seeker/me', {
    method: 'PATCH',
    headers: jsonHeaders,
    body: JSON.stringify(body),
  })
}

export function fetchExperience(): Promise<SeekerExperienceDto[]> {
  return apiFetch('/api/seeker/experience')
}

export function createExperience(body: CreateSeekerExperienceRequest): Promise<SeekerExperienceDto> {
  return apiFetch('/api/seeker/experience', {
    method: 'POST',
    headers: jsonHeaders,
    body: JSON.stringify(body),
  })
}

export function updateExperience(
  id: number,
  body: CreateSeekerExperienceRequest,
): Promise<SeekerExperienceDto> {
  return apiFetch(`/api/seeker/experience/${id}`, {
    method: 'PATCH',
    headers: jsonHeaders,
    body: JSON.stringify(body),
  })
}

export function deleteExperience(id: number): Promise<void> {
  return apiFetch(`/api/seeker/experience/${id}`, { method: 'DELETE' })
}

export function fetchEducation(): Promise<SeekerEducationDto[]> {
  return apiFetch('/api/seeker/education')
}

export function createEducation(body: CreateSeekerEducationRequest): Promise<SeekerEducationDto> {
  return apiFetch('/api/seeker/education', {
    method: 'POST',
    headers: jsonHeaders,
    body: JSON.stringify(body),
  })
}

export function updateEducation(
  id: number,
  body: CreateSeekerEducationRequest,
): Promise<SeekerEducationDto> {
  return apiFetch(`/api/seeker/education/${id}`, {
    method: 'PATCH',
    headers: jsonHeaders,
    body: JSON.stringify(body),
  })
}

export function deleteEducation(id: number): Promise<void> {
  return apiFetch(`/api/seeker/education/${id}`, { method: 'DELETE' })
}

export function fetchSeekerSkills(): Promise<SeekerSkillsResponse> {
  return apiFetch('/api/seeker/skills')
}

export function updateSeekerSkills(skillIds: number[]): Promise<SeekerSkillsResponse> {
  return apiFetch('/api/seeker/skills', {
    method: 'PUT',
    headers: jsonHeaders,
    body: JSON.stringify({ skillIds }),
  })
}

export function fetchDesiredPositions(): Promise<SeekerDesiredPositionsResponse> {
  return apiFetch('/api/seeker/desired-positions')
}

export function fetchPositionsOverview(): Promise<SeekerPositionsOverviewDto> {
  return apiFetch('/api/seeker/positions-overview')
}

export function updateDesiredPositions(
  occupationIds: number[],
): Promise<SeekerDesiredPositionsResponse> {
  return apiFetch('/api/seeker/desired-positions', {
    method: 'PUT',
    headers: jsonHeaders,
    body: JSON.stringify({ occupationIds }),
  })
}

export function fetchPersonalityPreview(): Promise<PersonalityPreviewDto> {
  return apiFetch('/api/seeker/personality-preview')
}

export function triggerPersonalityGeneration(): Promise<{ status: string }> {
  return apiFetch('/api/seeker/personality/generate', { method: 'POST' })
}

export function subscribePersonalityStatusEvents(
  onStatus: (status: PersonalityProfileStatus) => void,
  onError?: () => void,
): () => void {
  const eventSource = new EventSource('/api/seeker/personality-preview/events')
  eventSource.addEventListener('personality-status', (event) => {
    try {
      const payload = JSON.parse(event.data) as { status: PersonalityProfileStatus }
      onStatus(payload.status)
    } catch {
      // ignore malformed payloads
    }
  })
  eventSource.onerror = () => {
    eventSource.close()
    onError?.()
  }
  return () => eventSource.close()
}

export function fetchRecommendations(): Promise<JobRecommendationDto[]> {
  return apiFetch('/api/seeker/recommendations')
}

export function respondToJob(jobProfileId: number): Promise<JobRecommendationDto> {
  return apiFetch(`/api/seeker/recommendations/${jobProfileId}/respond`, { method: 'POST' })
}

export function fetchSeekerInterests(): Promise<SeekerInterestsResponseDto> {
  return apiFetch('/api/seeker/interests')
}

export function fetchSurveyGroups(): Promise<SurveyGroupsResponseDto> {
  return apiFetch('/api/seeker/surveys')
}

export function fetchSurvey(id: number): Promise<SurveyDetailDto> {
  return apiFetch(`/api/seeker/surveys/${id}`)
}

export function startSurvey(id: number): Promise<SurveyDetailDto> {
  return apiFetch(`/api/seeker/surveys/${id}/start`, { method: 'POST' })
}

export function saveSurveyAnswers(id: number, answers: Record<string, unknown>): Promise<SurveyDetailDto> {
  return apiFetch(`/api/seeker/surveys/${id}/answers`, {
    method: 'PUT',
    headers: jsonHeaders,
    body: JSON.stringify({ answers }),
  })
}

export function completeSurvey(id: number, answers: Record<string, unknown>): Promise<CompleteSurveyResponseDto> {
  return apiFetch(`/api/seeker/surveys/${id}/complete`, {
    method: 'POST',
    headers: jsonHeaders,
    body: JSON.stringify({ answers }),
  })
}
