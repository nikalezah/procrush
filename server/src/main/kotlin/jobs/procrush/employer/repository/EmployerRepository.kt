package jobs.procrush.employer.repository

import jobs.procrush.employer.dto.CreateJobProfileRequest
import jobs.procrush.employer.dto.EmployerProfileDto
import jobs.procrush.employer.dto.JobProfileDto
import jobs.procrush.employer.dto.UpdateEmployerProfileRequest
import jobs.procrush.employer.dto.UpdateJobProfileRequest
import jobs.procrush.employer.tables.EmployerJobProfilesTable
import jobs.procrush.employer.tables.EmployersTable
import jobs.procrush.employer.tables.JobProfileSkillsTable
import jobs.procrush.personality.dto.PersonalityAxesDto
import jobs.procrush.shared.repository.ReferenceRepository
import jobs.procrush.shared.tables.OccupationsTable
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.time.OffsetDateTime
import java.util.UUID

class EmployerRepository(
    private val referenceRepository: ReferenceRepository,
) {
    fun createForUser(userId: UUID, name: String = ""): EmployerProfileDto =
        transaction {
            val now = OffsetDateTime.now()
            val id =
                EmployersTable.insert {
                    it[EmployersTable.userId] = userId
                    it[EmployersTable.name] = name.trim()
                    it[updatedAt] = now
                }[EmployersTable.id].value
            findById(id)!!
        }

    fun findByUserId(userId: UUID): EmployerProfileDto? =
        transaction {
            EmployersTable
                .selectAll()
                .where { EmployersTable.userId eq userId }
                .firstOrNull()
                ?.toDto()
        }

    fun findById(employerId: Long): EmployerProfileDto? =
        transaction {
            EmployersTable
                .selectAll()
                .where { EmployersTable.id eq employerId }
                .firstOrNull()
                ?.toDto()
        }

    fun updateProfile(employerId: Long, request: UpdateEmployerProfileRequest): EmployerProfileDto? =
        transaction {
            val updated =
                EmployersTable.update({ EmployersTable.id eq employerId }) {
                    it[name] = request.name.trim()
                    it[description] = request.description?.trim()?.ifBlank { null }
                    it[website] = request.website?.trim()?.ifBlank { null }
                    it[phone] = request.phone?.trim()?.ifBlank { null }
                    it[emailContact] = request.emailContact?.trim()?.ifBlank { null }
                    it[updatedAt] = OffsetDateTime.now()
                }
            if (updated == 0) return@transaction null
            findById(employerId)
        }

    fun listJobProfiles(employerId: Long): List<JobProfileDto> =
        transaction {
            EmployerJobProfilesTable
                .selectAll()
                .where { EmployerJobProfilesTable.employerId eq employerId }
                .map { row -> row.toJobProfileDto() }
        }

    fun findJobProfile(employerId: Long, jobProfileId: Long): JobProfileDto? =
        transaction {
            EmployerJobProfilesTable
                .selectAll()
                .where {
                    (EmployerJobProfilesTable.id eq jobProfileId) and
                        (EmployerJobProfilesTable.employerId eq employerId)
                }
                .firstOrNull()
                ?.toJobProfileDto()
        }

    fun createJobProfile(employerId: Long, request: CreateJobProfileRequest): JobProfileDto =
        transaction {
            val personalityAxes = resolvePersonalityAxes(request.personalityAxes)
            val now = OffsetDateTime.now()
            val id =
                EmployerJobProfilesTable.insert {
                    it[EmployerJobProfilesTable.employerId] = employerId
                    it[occupationId] = request.occupationId
                    it[description] = request.description?.trim()?.ifBlank { null }
                    it[requiredPersonality] = PersonalityAxesDto.toJson(personalityAxes)
                    it[isActive] = request.isActive
                    it[createdAt] = now
                    it[updatedAt] = now
                }[EmployerJobProfilesTable.id].value
            setJobProfileSkills(id, request.skillIds)
            findJobProfile(employerId, id)!!
        }

    fun updateJobProfile(
        employerId: Long,
        jobProfileId: Long,
        request: UpdateJobProfileRequest,
    ): JobProfileDto? =
        transaction {
            val personalityAxes = resolvePersonalityAxes(request.personalityAxes)
            val updated =
                EmployerJobProfilesTable.update({
                    (EmployerJobProfilesTable.id eq jobProfileId) and
                        (EmployerJobProfilesTable.employerId eq employerId)
                }) {
                    it[occupationId] = request.occupationId
                    it[description] = request.description?.trim()?.ifBlank { null }
                    it[requiredPersonality] = PersonalityAxesDto.toJson(personalityAxes)
                    it[isActive] = request.isActive
                    it[updatedAt] = OffsetDateTime.now()
                }
            if (updated == 0) return@transaction null
            setJobProfileSkills(jobProfileId, request.skillIds)
            findJobProfile(employerId, jobProfileId)
        }

    fun deleteJobProfile(employerId: Long, jobProfileId: Long): Boolean =
        transaction {
            EmployerJobProfilesTable.deleteWhere {
                (EmployerJobProfilesTable.id eq jobProfileId) and
                    (EmployerJobProfilesTable.employerId eq employerId)
            } > 0
        }

    private fun setJobProfileSkills(jobProfileId: Long, skillIds: List<Long>) {
        JobProfileSkillsTable.deleteWhere { JobProfileSkillsTable.jobProfileId eq jobProfileId }
        skillIds.distinct().forEach { skillId ->
            JobProfileSkillsTable.insert {
                it[JobProfileSkillsTable.jobProfileId] = jobProfileId
                it[JobProfileSkillsTable.skillId] = skillId
            }
        }
    }

    private fun getJobProfileSkillIds(jobProfileId: Long): List<Long> =
        JobProfileSkillsTable
            .selectAll()
            .where { JobProfileSkillsTable.jobProfileId eq jobProfileId }
            .map { it[JobProfileSkillsTable.skillId].value }

    private fun ResultRow.toDto() =
        EmployerProfileDto(
            id = this[EmployersTable.id].value,
            name = this[EmployersTable.name],
            description = this[EmployersTable.description],
            website = this[EmployersTable.website],
            phone = this[EmployersTable.phone],
            emailContact = this[EmployersTable.emailContact],
        )

    private fun ResultRow.toJobProfileDto(): JobProfileDto {
        val jobProfileId = this[EmployerJobProfilesTable.id].value
        val occupationId = this[EmployerJobProfilesTable.occupationId].value
        val occupationName =
            OccupationsTable
                .selectAll()
                .where { OccupationsTable.id eq occupationId }
                .firstOrNull()
                ?.get(OccupationsTable.name)
                ?: "—"
        val skillIds = getJobProfileSkillIds(jobProfileId)
        return JobProfileDto(
            id = jobProfileId,
            occupationId = occupationId,
            occupationName = occupationName,
            description = this[EmployerJobProfilesTable.description],
            isActive = this[EmployerJobProfilesTable.isActive],
            skillIds = skillIds,
            skills = referenceRepository.findSkillsByIds(skillIds),
            personalityAxes = PersonalityAxesDto.fromJson(this[EmployerJobProfilesTable.requiredPersonality]),
        )
    }

    private fun resolvePersonalityAxes(axes: PersonalityAxesDto?): PersonalityAxesDto {
        val resolved = axes ?: PersonalityAxesDto.DEFAULT
        resolved.validate()
        return resolved
    }
}
