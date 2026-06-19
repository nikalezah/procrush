export type UserRole = 'SEEKER' | 'EMPLOYER'

export interface AuthUserDto {
  id: string
  email: string
  profileName?: string | null
  role?: UserRole | null
}

export interface DevLoginRequest {
  email: string
}

export interface CompleteRegistrationRequest {
  email?: string | null
  role: UserRole
  firstName?: string | null
  lastName?: string | null
  middleName?: string | null
  companyName?: string | null
}

export interface MeResponse {
  user: AuthUserDto | null
}

export type AuthState =
  | { kind: 'loading' }
  | { kind: 'unauthenticated' }
  | { kind: 'needsRegistration'; user: AuthUserDto }
  | { kind: 'authenticated'; user: AuthUserDto }

export interface OccupationDto {
  id: number
  parentId: number | null
  name: string
  isLeaf: boolean
}

export interface SkillDto {
  id: number
  name: string
}

export interface SeekerProfileDto {
  id: number
  firstName: string
  middleName: string | null
  lastName: string
  phone: string | null
  telegram: string | null
  linkedin: string | null
}

export interface UpdateSeekerProfileRequest {
  firstName: string
  middleName?: string | null
  lastName: string
  phone?: string | null
  telegram?: string | null
  linkedin?: string | null
}

export interface SeekerExperienceDto {
  id: number
  companyName: string
  position: string
  description: string | null
  startDate: string
  endDate: string | null
}

export interface CreateSeekerExperienceRequest {
  companyName: string
  position: string
  description?: string | null
  startDate: string
  endDate?: string | null
}

export interface SeekerEducationDto {
  id: number
  institution: string
  degree: string | null
  specialization: string
  endYear: number
}

export interface CreateSeekerEducationRequest {
  institution: string
  degree?: string | null
  specialization: string
  endYear: number
}

export interface SeekerSkillsResponse {
  skillIds: number[]
  skills: SkillDto[]
}

export interface SeekerDesiredPositionsResponse {
  occupationIds: number[]
  occupations: OccupationDto[]
}

export interface EmployerProfileDto {
  id: number
  name: string
  description: string | null
  website: string | null
  phone: string | null
  emailContact: string | null
}

export interface UpdateEmployerProfileRequest {
  name: string
  description?: string | null
  website?: string | null
  phone?: string | null
  emailContact?: string | null
}

export interface JobProfileDto {
  id: number
  occupationId: number
  occupationName: string
  description: string | null
  isActive: boolean
  skillIds: number[]
  skills: SkillDto[]
  personalityAxes: PersonalityAxesDto
}

export interface PersonalityAxesDto {
  axisDominance: number
  axisInfluence: number
  axisStability: number
  axisIntegrity: number
  axisAutonomy: number
  axisPace: number
}

export const DEFAULT_PERSONALITY_AXES: PersonalityAxesDto = {
  axisDominance: 0.5,
  axisInfluence: 0.5,
  axisStability: 0.5,
  axisIntegrity: 0.5,
  axisAutonomy: 0.5,
  axisPace: 0.5,
}

export interface CreateJobProfileRequest {
  occupationId: number
  description?: string | null
  isActive?: boolean
  skillIds?: number[]
  personalityAxes?: PersonalityAxesDto | null
}

export interface SucceedThroughDto {
  point0: string
  point1: string
  point2: string
}

export interface PersonalityTraitDetailsDto {
  description: string
  goodDay: string
  badDay: string
  succeedThrough: SucceedThroughDto
}

export interface PersonalityTraitDto {
  key: string
  label: string
  scalePosition: number
  leftPole: string
  rightPole: string
  details: PersonalityTraitDetailsDto
  isTopStrength?: boolean
}

export interface PersonalityCategoryDto {
  key: string
  description: string
  topStrengthIndex: number
  traits: PersonalityTraitDto[]
}

export interface PersonalityItemDto {
  title: string
  description: string
}

export interface PersonalitySectionDto {
  title: string
  items: PersonalityItemDto[]
}

/** Ровно 3 источника энергии (см. серверные PersonalitySectionRules). */
export interface EnergySourcesSectionDto {
  title: 'Источники энергии'
  items: [PersonalityItemDto, PersonalityItemDto, PersonalityItemDto]
}

/** Ровно 2 стоп-фактора (см. серверные PersonalitySectionRules). */
export interface StopFactorsSectionDto {
  title: 'Стоп-факторы'
  items: [PersonalityItemDto, PersonalityItemDto]
}

export interface SuperpowerAndTalentDto {
  id: number
  name: string
  isPronounced: boolean
}

export type PersonalityProfileStatus = 'NOT_READY' | 'PROCESSING' | 'READY' | 'FAILED'

export interface PersonalityPreviewDto {
  status: PersonalityProfileStatus
  generationError?: string | null
  testsCompleted: number
  testsTotal: number
  title?: string | null
  description?: string | null
  profile?: string | null
  autonomy?: string | null
  thinkingStyle?: string | null
  burnoutRisk?: string | null
  axisDominance?: number | null
  axisInfluence?: number | null
  axisStability?: number | null
  axisIntegrity?: number | null
  axisAutonomy?: number | null
  axisPace?: number | null
  categories?: PersonalityCategoryDto[] | null
  energySources?: EnergySourcesSectionDto | null
  stopFactors?: StopFactorsSectionDto | null
  superpowersAndTalents?: SuperpowerAndTalentDto[] | null
}

export type InterestStatus = 'NONE' | 'RESPONDED' | 'INCOMING' | 'MUTUAL'

export interface MatchInterestEventDto {
  jobProfileId: number
  seekerId: number
  interestStatus: InterestStatus
  employerContact?: EmployerContactDto | null
  seekerContact?: SeekerContactDto | null
}

export interface MatchInterestCountDto {
  count: number
}

export interface EmployerContactDto {
  companyName: string
  phone?: string | null
  emailContact?: string | null
  website?: string | null
}

export interface SeekerContactDto {
  firstName: string
  lastName: string
  phone?: string | null
  telegram?: string | null
  linkedin?: string | null
}

export interface JobRecommendationDto {
  id: number
  companyName: string
  positionName: string
  description: string
  matchScore: number
  matchScoreDisplay: number
  interestStatus?: InterestStatus
  contactInfo?: EmployerContactDto | null
}

export interface CandidateRecommendationDto {
  id: number
  firstName: string
  lastName: string
  positionName: string
  skills: string[]
  matchScore: number
  matchScoreDisplay: number
  interestStatus?: InterestStatus
  contactInfo?: SeekerContactDto | null
}

export interface SeekerInterestsResponseDto {
  respondedOutside: JobRecommendationDto[]
  mutualOutside: JobRecommendationDto[]
}

export interface EmployerInterestsResponseDto {
  respondedOutside: CandidateRecommendationDto[]
  mutualOutside: CandidateRecommendationDto[]
}

export interface SeekerPositionsOverviewDto {
  occupationIds: number[]
  occupations: OccupationDto[]
  recommendations: JobRecommendationDto[]
  interests: SeekerInterestsResponseDto
  testsComplete: boolean
}

export interface EmployerCandidatesOverviewDto {
  candidates: CandidateRecommendationDto[]
  interests: EmployerInterestsResponseDto
}

export interface SeekerDashboardDto {
  profileCompletionPercent: number
  desiredPositionsCount: number
  experienceCount: number
  recommendationsPreview: JobRecommendationDto[]
  testsComplete: boolean
}

export interface EmployerDashboardDto {
  companyName: string
  jobProfilesCount: number
  activeJobProfilesCount: number
  totalMatchedCandidates: number
}

export type SurveyStatus = 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED'

export interface SurveyListItemDto {
  id: number
  code: string
  name: string
  description: string
  status: SurveyStatus
  sortOrder: number
  locked: boolean
}

export interface SurveyGroupDto {
  code: string
  name: string
  surveys: SurveyListItemDto[]
  completedCount: number
  totalCount: number
  status: SurveyStatus
  locked: boolean
  entrySurveyId: number | null
}

export interface SurveyGroupsResponseDto {
  groups: SurveyGroupDto[]
  testsCompleted: number
  testsTotal: number
}

export interface SurveyDetailDto {
  id: number
  code: string
  name: string
  description: string
  groupCode: string
  questionsJson: string
  status: SurveyStatus
  answersJson: string | null
  resultId: number | null
  locked: boolean
  stepNumber?: number | null
  stepTotal?: number | null
  prevSurveyId?: number | null
  nextSurveyId?: number | null
}

export interface CompleteSurveyResponseDto {
  resultId: number
  surveyId: number
  status: SurveyStatus
  nextSurveyId?: number | null
}

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
