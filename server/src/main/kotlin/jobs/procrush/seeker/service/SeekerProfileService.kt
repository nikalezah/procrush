package jobs.procrush.seeker.service

import jobs.procrush.matching.service.MatchingService
import jobs.procrush.seeker.dto.CreateSeekerEducationRequest
import jobs.procrush.seeker.dto.CreateSeekerExperienceRequest
import jobs.procrush.seeker.dto.SeekerDashboardDto
import jobs.procrush.seeker.dto.SeekerDesiredPositionsResponse
import jobs.procrush.seeker.dto.SeekerSkillsResponse
import jobs.procrush.seeker.dto.UpdateSeekerEducationRequest
import jobs.procrush.seeker.dto.UpdateSeekerExperienceRequest
import jobs.procrush.seeker.dto.UpdateSeekerProfileRequest
import jobs.procrush.seeker.repository.SeekerRepository
import jobs.procrush.shared.ResourceNotFoundException
import jobs.procrush.shared.repository.ReferenceRepository
import java.util.UUID

class SeekerProfileService(
    private val seekerRepository: SeekerRepository,
    private val referenceRepository: ReferenceRepository,
    private val matchingService: MatchingService,
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

        return SeekerDashboardDto(
            profileCompletionPercent = (filled * 100) / total,
            desiredPositionsCount = desired.size,
            experienceCount = experience.size,
            recommendationsPreview = recommendations.take(2),
        )
    }

    fun recommendations(userId: UUID) = matchingService.jobRecommendationsForSeeker(userId)
}
