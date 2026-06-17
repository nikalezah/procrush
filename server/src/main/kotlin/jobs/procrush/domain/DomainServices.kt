package jobs.procrush.domain

import jobs.procrush.auth.UserRole
import jobs.procrush.db.EmployerRepository
import jobs.procrush.db.ReferenceRepository
import jobs.procrush.db.SeekerRepository
import jobs.procrush.fixtures.StubData
import jobs.procrush.models.CreateSeekerEducationRequest
import jobs.procrush.models.CreateSeekerExperienceRequest
import jobs.procrush.models.EmployerDashboardDto
import jobs.procrush.models.SeekerDashboardDto
import jobs.procrush.models.SeekerDesiredPositionsResponse
import jobs.procrush.models.SeekerSkillsResponse
import jobs.procrush.models.UpdateSeekerEducationRequest
import jobs.procrush.models.UpdateSeekerExperienceRequest
import jobs.procrush.models.UpdateSeekerProfileRequest
import java.util.UUID

class ProfileProvisioningService(
    private val seekerRepository: SeekerRepository,
    private val employerRepository: EmployerRepository,
) {
    fun provisionForRole(
        userId: UUID,
        role: UserRole,
        firstName: String = "",
        lastName: String = "",
        middleName: String? = null,
        companyName: String = "",
    ) {
        when (role) {
            UserRole.SEEKER -> {
                if (seekerRepository.findByUserId(userId) == null) {
                    seekerRepository.createForUser(
                        userId = userId,
                        firstName = firstName,
                        lastName = lastName,
                        middleName = middleName,
                    )
                }
            }
            UserRole.EMPLOYER -> {
                if (employerRepository.findByUserId(userId) == null) {
                    employerRepository.createForUser(userId, name = companyName)
                }
            }
        }
    }
}

class SeekerProfileService(
    private val seekerRepository: SeekerRepository,
    private val referenceRepository: ReferenceRepository,
    private val surveyService: SurveyService,
    private val personalityProfileService: PersonalityProfileService,
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
            ?: error("Не удалось обновить профиль")
    }

    fun listExperience(userId: UUID) =
        seekerRepository.listExperience(getOrCreateSeeker(userId).id)

    fun createExperience(userId: UUID, request: CreateSeekerExperienceRequest) =
        seekerRepository.createExperience(getOrCreateSeeker(userId).id, request)

    fun updateExperience(userId: UUID, experienceId: Long, request: UpdateSeekerExperienceRequest) =
        seekerRepository.updateExperience(getOrCreateSeeker(userId).id, experienceId, request)
            ?: error("Запись опыта не найдена")

    fun deleteExperience(userId: UUID, experienceId: Long) {
        if (!seekerRepository.deleteExperience(getOrCreateSeeker(userId).id, experienceId)) {
            error("Запись опыта не найдена")
        }
    }

    fun listEducation(userId: UUID) =
        seekerRepository.listEducation(getOrCreateSeeker(userId).id)

    fun createEducation(userId: UUID, request: CreateSeekerEducationRequest) =
        seekerRepository.createEducation(getOrCreateSeeker(userId).id, request)

    fun updateEducation(userId: UUID, educationId: Long, request: UpdateSeekerEducationRequest) =
        seekerRepository.updateEducation(getOrCreateSeeker(userId).id, educationId, request)
            ?: error("Запись об образовании не найдена")

    fun deleteEducation(userId: UUID, educationId: Long) {
        if (!seekerRepository.deleteEducation(getOrCreateSeeker(userId).id, educationId)) {
            error("Запись об образовании не найдена")
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
                ?: error("Должность не найдена: $id")
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

        return SeekerDashboardDto(
            profileCompletionPercent = (filled * 100) / total,
            desiredPositionsCount = desired.size,
            experienceCount = experience.size,
            recommendationsPreview = StubData.jobRecommendations().take(2),
        )
    }

    fun personalityPreview(userId: UUID) = personalityProfileService.getPreview(userId)

    fun recommendations() = StubData.jobRecommendations()
}

class EmployerProfileService(
    private val employerRepository: EmployerRepository,
    private val referenceRepository: ReferenceRepository,
) {
    fun getOrCreateEmployer(userId: UUID) =
        employerRepository.findByUserId(userId) ?: employerRepository.createForUser(userId)

    fun updateProfile(userId: UUID, request: jobs.procrush.models.UpdateEmployerProfileRequest) {
        val name = request.name.trim()
        require(name.isNotBlank()) { "Укажите название компании" }
        val employer = getOrCreateEmployer(userId)
        employerRepository.updateProfile(employer.id, request)
            ?: error("Не удалось обновить профиль компании")
    }

    fun listJobProfiles(userId: UUID) =
        employerRepository.listJobProfiles(getOrCreateEmployer(userId).id)

    fun createJobProfile(userId: UUID, request: jobs.procrush.models.CreateJobProfileRequest): jobs.procrush.models.JobProfileDto {
        referenceRepository.findOccupationById(request.occupationId)
            ?: error("Должность не найдена")
        return employerRepository.createJobProfile(getOrCreateEmployer(userId).id, request)
    }

    fun updateJobProfile(userId: UUID, jobProfileId: Long, request: jobs.procrush.models.UpdateJobProfileRequest) {
        referenceRepository.findOccupationById(request.occupationId)
            ?: error("Должность не найдена")
        employerRepository.updateJobProfile(getOrCreateEmployer(userId).id, jobProfileId, request)
            ?: error("Профиль не найден")
    }

    fun deleteJobProfile(userId: UUID, jobProfileId: Long) {
        if (!employerRepository.deleteJobProfile(getOrCreateEmployer(userId).id, jobProfileId)) {
            error("Профиль не найден")
        }
    }

    fun findJobProfile(userId: UUID, jobProfileId: Long) =
        employerRepository.findJobProfile(getOrCreateEmployer(userId).id, jobProfileId)
            ?: error("Профиль не найден")

    fun dashboard(userId: UUID): EmployerDashboardDto {
        val employer = getOrCreateEmployer(userId)
        val profiles = employerRepository.listJobProfiles(employer.id)
        return EmployerDashboardDto(
            companyName = employer.name.ifBlank { "Компания не указана" },
            jobProfilesCount = profiles.size,
            activeJobProfilesCount = profiles.count { it.isActive },
            totalCandidatesStub = profiles.size * 3,
        )
    }

    fun candidates(jobProfileId: Long) = StubData.candidateRecommendations(jobProfileId)
}
