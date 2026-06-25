package jobs.procrush.seeker.service

import jobs.procrush.matching.cache.CachedMatchingService
import jobs.procrush.matching.dto.SeekerInterestsResponseDto
import jobs.procrush.matching.port.MatchingCachePort
import jobs.procrush.matching.port.MatchingEventPort
import jobs.procrush.matching.service.MatchInterestService
import jobs.procrush.seeker.dto.CreateSeekerEducationRequest
import jobs.procrush.seeker.dto.CreateSeekerExperienceRequest
import jobs.procrush.seeker.dto.SeekerDashboardDto
import jobs.procrush.seeker.dto.SeekerDesiredPositionsResponse
import jobs.procrush.seeker.dto.SeekerPositionsOverviewDto
import jobs.procrush.seeker.dto.SeekerSkillsResponse
import jobs.procrush.seeker.dto.UpdateSeekerEducationRequest
import jobs.procrush.seeker.dto.UpdateSeekerExperienceRequest
import jobs.procrush.seeker.dto.UpdateSeekerProfileRequest
import jobs.procrush.seeker.repository.SeekerRepository
import jobs.procrush.shared.ResourceNotFoundException
import jobs.procrush.shared.repository.ReferenceRepository
import jobs.procrush.survey.service.SurveyService
import java.util.UUID

class SeekerProfileService(
    private val seekerRepository: SeekerRepository,
    private val referenceRepository: ReferenceRepository,
    private val matchingService: CachedMatchingService,
    private val matchInterestService: MatchInterestService,
    private val surveyService: SurveyService,
    private val matchingCache: MatchingCachePort,
    private val matchingEvents: MatchingEventPort,
) {
    fun getOrCreateSeeker(userId: UUID) =
        seekerRepository.findByUserId(userId) ?: seekerRepository.createForUser(userId)

    fun updateProfile(userId: UUID, request: UpdateSeekerProfileRequest) {
        val firstName = request.firstName.trim()
        val lastName = request.lastName.trim()
        require(firstName.isNotBlank()) { "Укажите имя" }
        require(lastName.isNotBlank()) { "Укажите фамилию" }
        val seeker = getOrCreateSeeker(userId)
        seekerRepository.updateProfile(seeker.id, request)
            ?: throw ResourceNotFoundException("Не удалось обновить профиль")
    }

    fun listExperience(userId: UUID) =
        seekerRepository.listExperience(getOrCreateSeeker(userId).id)

    fun createExperience(userId: UUID, request: CreateSeekerExperienceRequest) =
        seekerRepository.createExperience(getOrCreateSeeker(userId).id, request)

    fun updateExperience(userId: UUID, experienceId: Long, request: UpdateSeekerExperienceRequest) =
        seekerRepository.updateExperience(getOrCreateSeeker(userId).id, experienceId, request)
            ?: throw ResourceNotFoundException("Запись опыта не найдена")

    fun deleteExperience(userId: UUID, experienceId: Long) {
        if (!seekerRepository.deleteExperience(getOrCreateSeeker(userId).id, experienceId)) {
            throw ResourceNotFoundException("Запись опыта не найдена")
        }
    }

    fun listEducation(userId: UUID) =
        seekerRepository.listEducation(getOrCreateSeeker(userId).id)

    fun createEducation(userId: UUID, request: CreateSeekerEducationRequest) =
        seekerRepository.createEducation(getOrCreateSeeker(userId).id, request)

    fun updateEducation(userId: UUID, educationId: Long, request: UpdateSeekerEducationRequest) =
        seekerRepository.updateEducation(getOrCreateSeeker(userId).id, educationId, request)
            ?: throw ResourceNotFoundException("Запись об образовании не найдена")

    fun deleteEducation(userId: UUID, educationId: Long) {
        if (!seekerRepository.deleteEducation(getOrCreateSeeker(userId).id, educationId)) {
            throw ResourceNotFoundException("Запись об образовании не найдена")
        }
    }

    fun getSkills(userId: UUID): SeekerSkillsResponse {
        val seeker = getOrCreateSeeker(userId)
        val skillIds = seekerRepository.getSkillIds(seeker.id)
        val skills = referenceRepository.findSkillsByIds(skillIds)
        return SeekerSkillsResponse(skillIds = skillIds, skills = skills)
    }

    fun setSkills(userId: UUID, skillIds: List<Long>): SeekerSkillsResponse {
        val seeker = getOrCreateSeeker(userId)
        seekerRepository.setSkillIds(seeker.id, skillIds)
        matchingCache.invalidateSeekerJobs(seeker.id)
        matchingEvents.publishSeekerProfileChanged(seeker.id)
        return getSkills(userId)
    }

    fun getDesiredPositions(userId: UUID): SeekerDesiredPositionsResponse {
        val seeker = getOrCreateSeeker(userId)
        val occupationIds = seekerRepository.getDesiredOccupationIds(seeker.id)
        val occupations = referenceRepository.findOccupationsByIds(occupationIds)
        return SeekerDesiredPositionsResponse(occupationIds = occupationIds, occupations = occupations)
    }

    fun setDesiredPositions(userId: UUID, occupationIds: List<Long>): SeekerDesiredPositionsResponse {
        val seeker = getOrCreateSeeker(userId)
        occupationIds.forEach { id ->
            referenceRepository.findOccupationById(id)
                ?: throw IllegalArgumentException("Должность не найдена: $id")
        }
        seekerRepository.setDesiredOccupationIds(seeker.id, occupationIds)
        matchingCache.invalidateSeekerJobs(seeker.id)
        matchingEvents.publishSeekerProfileChanged(seeker.id)
        return getDesiredPositions(userId)
    }

    fun dashboard(userId: UUID): SeekerDashboardDto {
        val seeker = getOrCreateSeeker(userId)
        val experience = seekerRepository.listExperience(seeker.id)
        val education = seekerRepository.listEducation(seeker.id)
        val skillIds = seekerRepository.getSkillIds(seeker.id)
        val desired = seekerRepository.getDesiredOccupationIds(seeker.id)

        var filled = 0
        val total = 6
        if (seeker.firstName.isNotBlank() && seeker.lastName.isNotBlank()) filled++
        if (seeker.phone != null || seeker.telegram != null || seeker.linkedin != null) filled++
        if (experience.isNotEmpty()) filled++
        if (education.isNotEmpty()) filled++
        if (skillIds.isNotEmpty()) filled++
        if (desired.isNotEmpty()) filled++

        val recommendations = matchingService.jobRecommendationsForSeeker(userId)
        val surveyGroups = surveyService.listGroups(userId)
        val testsComplete = surveyGroups.testsCompleted >= surveyGroups.testsTotal

        return SeekerDashboardDto(
            profileCompletionPercent = (filled * 100) / total,
            desiredPositionsCount = desired.size,
            experienceCount = experience.size,
            recommendationsPreview = recommendations.take(2),
            testsComplete = testsComplete,
        )
    }

    fun recommendations(userId: UUID): List<jobs.procrush.matching.dto.JobRecommendationDto> {
        val seeker = getOrCreateSeeker(userId)
        val recommendations = matchingService.jobRecommendationsForSeeker(userId)
        return matchInterestService.enrichJobRecommendations(seeker.id, recommendations)
    }

    fun respondToJob(userId: UUID, jobProfileId: Long) =
        matchInterestService.seekerRespond(userId, jobProfileId)

    fun interestsOutsideRecommendations(userId: UUID): SeekerInterestsResponseDto {
        val recommendations = matchingService.jobRecommendationsForSeeker(userId)
        return matchInterestService.seekerInterestsOutsideRecommendations(
            userId,
            recommendations.map { it.id }.toSet(),
        )
    }

    fun positionsOverview(userId: UUID): SeekerPositionsOverviewDto {
        val seeker = getOrCreateSeeker(userId)
        val surveyGroups = surveyService.listGroups(userId)
        val testsComplete = surveyGroups.testsCompleted >= surveyGroups.testsTotal
        val occupationIds = seekerRepository.getDesiredOccupationIds(seeker.id)
        val occupations = referenceRepository.listOccupations(leafOnly = true)
        val recommendations = matchingService.jobRecommendationsForSeeker(userId)
        val enriched = matchInterestService.enrichJobRecommendations(seeker.id, recommendations)
        val interests =
            matchInterestService.seekerInterestsOutsideRecommendations(
                userId,
                enriched.map { it.id }.toSet(),
            )
        return SeekerPositionsOverviewDto(
            occupationIds = occupationIds,
            occupations = occupations,
            recommendations = enriched,
            interests = interests,
            testsComplete = testsComplete,
        )
    }
}
