import type {components} from './generated/schema'

type Schemas = components['schemas']

// Re-export API DTO types from generated OpenAPI schema
export type UserRole = Schemas['UserRole']
export type InterestStatus = Schemas['InterestStatus']
export type PersonalityProfileStatus = Schemas['PersonalityProfileStatus']
export type SurveyStatus = Schemas['SurveyStatus']

export type AuthUserDto = Schemas['AuthUserDto']
export type DevLoginRequest = Schemas['DevLoginRequest']
export type CompleteRegistrationRequest = Schemas['CompleteRegistrationRequest']
export type MeResponse = Schemas['MeResponse']

export type OccupationDto = Schemas['OccupationDto']
export type SkillDto = Schemas['SkillDto']

export type SeekerProfileDto = Schemas['SeekerProfileDto']
export type UpdateSeekerProfileRequest = Schemas['UpdateSeekerProfileRequest']
export type SeekerExperienceDto = Schemas['SeekerExperienceDto']
export type CreateSeekerExperienceRequest = Schemas['CreateSeekerExperienceRequest']
export type SeekerEducationDto = Schemas['SeekerEducationDto']
export type CreateSeekerEducationRequest = Schemas['CreateSeekerEducationRequest']
export type SeekerSkillsResponse = Schemas['SeekerSkillsResponse']
export type SeekerDesiredPositionsResponse = Schemas['SeekerDesiredPositionsResponse']
export type SeekerDashboardDto = Schemas['SeekerDashboardDto']
export type SeekerPositionsOverviewDto = Schemas['SeekerPositionsOverviewDto']
export type SeekerInterestsResponseDto = Schemas['SeekerInterestsResponseDto']

export type EmployerProfileDto = Schemas['EmployerProfileDto']
export type UpdateEmployerProfileRequest = Schemas['UpdateEmployerProfileRequest']
export type JobProfileDto = Schemas['JobProfileDto']
export type CreateJobProfileRequest = Schemas['CreateJobProfileRequest']
export type EmployerDashboardDto = Schemas['EmployerDashboardDto']
export type EmployerInterestsResponseDto = Schemas['EmployerInterestsResponseDto']
export type EmployerCandidatesOverviewDto = Schemas['EmployerCandidatesOverviewDto']

export type PersonalityAxesDto = Schemas['PersonalityAxesDto']
export type SucceedThroughDto = Schemas['SucceedThroughDto']
export type PersonalityTraitDetailsDto = Schemas['PersonalityTraitDetailsDto']
export type PersonalityTraitDto = Schemas['PersonalityTraitDto']
export type PersonalityCategoryDto = Schemas['PersonalityCategoryDto']
export type PersonalityItemDto = Schemas['PersonalityItemDto']
export type PersonalitySectionDto = Schemas['PersonalitySectionDto']
export type SuperpowerAndTalentDto = Schemas['SuperpowerAndTalentDto']
export type PersonalityPreviewDto = Schemas['PersonalityPreviewDto']

export type MatchInterestEventDto = Schemas['MatchInterestEventDto']
export type MatchInterestCountDto = Schemas['MatchInterestCountDto']
export type EmployerContactDto = Schemas['EmployerContactDto']
export type SeekerContactDto = Schemas['SeekerContactDto']
export type JobRecommendationDto = Schemas['JobRecommendationDto']
export type CandidateRecommendationDto = Schemas['CandidateRecommendationDto']

export type SurveyListItemDto = Schemas['SurveyListItemDto']
export type SurveyGroupDto = Schemas['SurveyGroupDto']
export type SurveyGroupsResponseDto = Schemas['SurveyGroupsResponseDto']
export type SurveyDetailDto = Schemas['SurveyDetailDto']
export type CompleteSurveyResponseDto = Schemas['CompleteSurveyResponseDto']

export const DEFAULT_PERSONALITY_AXES: PersonalityAxesDto = {
  axisDominance: 0.5,
  axisInfluence: 0.5,
  axisStability: 0.5,
  axisIntegrity: 0.5,
  axisAutonomy: 0.5,
  axisPace: 0.5,
}

// UI-only types (not part of OpenAPI schema)
export type AuthState =
  | { kind: 'loading' }
  | { kind: 'unauthenticated' }
  | { kind: 'needsRegistration'; user: AuthUserDto }
  | { kind: 'authenticated'; user: AuthUserDto }

export interface SurveyOption {
  id: number
  label: string
}

export interface SurveyQuestionBase {
  id: number
  text?: string
  option1?: string
  option2?: string
  options?: SurveyOption[]
}

export interface SurveyQuestionsDefinition {
  type: string
  instruction: string
  answerKey?: string
  minSelections?: number
  maxSelections?: number
  totalPoints?: number
  maxPerOption?: number
  questions?: SurveyQuestionBase[]
  options?: SurveyOption[]
}
