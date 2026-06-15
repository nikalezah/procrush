import { apiFetch } from './client'
import type { OccupationDto, SkillDto } from './types'

export function fetchOccupations(leafOnly = false): Promise<OccupationDto[]> {
  const params = leafOnly ? '?leafOnly=true' : ''
  return apiFetch(`/api/occupations${params}`)
}

export function searchSkills(query?: string): Promise<SkillDto[]> {
  const params = query ? `?q=${encodeURIComponent(query)}` : ''
  return apiFetch(`/api/skills${params}`)
}
