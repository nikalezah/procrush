import { apiFetch } from './client'
import type {
  CandidateRecommendationDto,
  CreateJobProfileRequest,
  EmployerDashboardDto,
  EmployerProfileDto,
  JobProfileDto,
  UpdateEmployerProfileRequest,
} from './types'

const jsonHeaders = { 'Content-Type': 'application/json' }

export function fetchEmployerDashboard(): Promise<EmployerDashboardDto> {
  return apiFetch('/api/employer/dashboard')
}

export function fetchEmployerProfile(): Promise<EmployerProfileDto> {
  return apiFetch('/api/employer/me')
}

export function updateEmployerProfile(body: UpdateEmployerProfileRequest): Promise<EmployerProfileDto> {
  return apiFetch('/api/employer/me', {
    method: 'PATCH',
    headers: jsonHeaders,
    body: JSON.stringify(body),
  })
}

export function fetchJobProfiles(): Promise<JobProfileDto[]> {
  return apiFetch('/api/employer/job-profiles')
}

export function createJobProfile(body: CreateJobProfileRequest): Promise<JobProfileDto> {
  return apiFetch('/api/employer/job-profiles', {
    method: 'POST',
    headers: jsonHeaders,
    body: JSON.stringify(body),
  })
}

export function updateJobProfile(id: number, body: CreateJobProfileRequest): Promise<JobProfileDto> {
  return apiFetch(`/api/employer/job-profiles/${id}`, {
    method: 'PATCH',
    headers: jsonHeaders,
    body: JSON.stringify(body),
  })
}

export function deleteJobProfile(id: number): Promise<void> {
  return apiFetch(`/api/employer/job-profiles/${id}`, { method: 'DELETE' })
}

export function fetchCandidates(jobProfileId: number): Promise<CandidateRecommendationDto[]> {
  return apiFetch(`/api/employer/job-profiles/${jobProfileId}/candidates`)
}
