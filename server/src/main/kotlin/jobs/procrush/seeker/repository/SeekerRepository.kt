package jobs.procrush.seeker.repository

import jobs.procrush.seeker.dto.CreateSeekerEducationRequest
import jobs.procrush.seeker.dto.CreateSeekerExperienceRequest
import jobs.procrush.seeker.dto.SeekerEducationDto
import jobs.procrush.seeker.dto.SeekerExperienceDto
import jobs.procrush.seeker.dto.SeekerProfileDto
import jobs.procrush.seeker.dto.UpdateSeekerEducationRequest
import jobs.procrush.seeker.dto.UpdateSeekerExperienceRequest
import jobs.procrush.seeker.dto.UpdateSeekerProfileRequest
import jobs.procrush.seeker.tables.SeekerDesiredPositionsTable
import jobs.procrush.seeker.tables.SeekerEducationTable
import jobs.procrush.seeker.tables.SeekerExperienceTable
import jobs.procrush.seeker.tables.SeekerSkillsTable
import jobs.procrush.seeker.tables.SeekersTable
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class SeekerRepository {
    fun createForUser(
        userId: UUID,
        firstName: String = "",
        lastName: String = "",
        middleName: String? = null,
    ): SeekerProfileDto =
        transaction {
            val now = OffsetDateTime.now()
            val id =
                SeekersTable.insert {
                    it[SeekersTable.userId] = userId
                    it[SeekersTable.firstName] = firstName.trim()
                    it[SeekersTable.lastName] = lastName.trim()
                    it[SeekersTable.middleName] = middleName?.trim()?.ifBlank { null }
                    it[updatedAt] = now
                }[SeekersTable.id].value
            findById(id)!!
        }

    fun findByUserId(userId: UUID): SeekerProfileDto? =
        transaction {
            SeekersTable
                .selectAll()
                .where { SeekersTable.userId eq userId }
                .firstOrNull()
                ?.toDto()
        }

    fun findById(seekerId: Long): SeekerProfileDto? =
        transaction {
            SeekersTable
                .selectAll()
                .where { SeekersTable.id eq seekerId }
                .firstOrNull()
                ?.toDto()
        }

    fun updateProfile(seekerId: Long, request: UpdateSeekerProfileRequest): SeekerProfileDto? =
        transaction {
            val updated =
                SeekersTable.update({ SeekersTable.id eq seekerId }) {
                    it[firstName] = request.firstName.trim()
                    it[middleName] = request.middleName?.trim()?.ifBlank { null }
                    it[lastName] = request.lastName.trim()
                    it[phone] = request.phone?.trim()?.ifBlank { null }
                    it[telegram] = request.telegram?.trim()?.ifBlank { null }
                    it[linkedin] = request.linkedin?.trim()?.ifBlank { null }
                    it[updatedAt] = OffsetDateTime.now()
                }
            if (updated == 0) return@transaction null
            findById(seekerId)
        }

    fun listExperience(seekerId: Long): List<SeekerExperienceDto> =
        transaction {
            SeekerExperienceTable
                .selectAll()
                .where { SeekerExperienceTable.seekerId eq seekerId }
                .map { it.toExperienceDto() }
        }

    fun createExperience(seekerId: Long, request: CreateSeekerExperienceRequest): SeekerExperienceDto =
        transaction {
            val now = OffsetDateTime.now()
            val id =
                SeekerExperienceTable.insert {
                    it[SeekerExperienceTable.seekerId] = seekerId
                    it[companyName] = request.companyName.trim()
                    it[position] = request.position.trim()
                    it[description] = request.description?.trim()?.ifBlank { null }
                    it[startDate] = LocalDate.parse(request.startDate)
                    it[endDate] = request.endDate?.let(LocalDate::parse)
                    it[createdAt] = now
                    it[updatedAt] = now
                }[SeekerExperienceTable.id].value
            SeekerExperienceTable
                .selectAll()
                .where { SeekerExperienceTable.id eq id }
                .first()
                .toExperienceDto()
        }

    fun updateExperience(
        seekerId: Long,
        experienceId: Long,
        request: UpdateSeekerExperienceRequest,
    ): SeekerExperienceDto? =
        transaction {
            val updated =
                SeekerExperienceTable.update({
                    (SeekerExperienceTable.id eq experienceId) and (SeekerExperienceTable.seekerId eq seekerId)
                }) {
                    it[companyName] = request.companyName.trim()
                    it[position] = request.position.trim()
                    it[description] = request.description?.trim()?.ifBlank { null }
                    it[startDate] = LocalDate.parse(request.startDate)
                    it[endDate] = request.endDate?.let(LocalDate::parse)
                    it[updatedAt] = OffsetDateTime.now()
                }
            if (updated == 0) return@transaction null
            SeekerExperienceTable
                .selectAll()
                .where { SeekerExperienceTable.id eq experienceId }
                .first()
                .toExperienceDto()
        }

    fun deleteExperience(seekerId: Long, experienceId: Long): Boolean =
        transaction {
            SeekerExperienceTable.deleteWhere {
                (SeekerExperienceTable.id eq experienceId) and (SeekerExperienceTable.seekerId eq seekerId)
            } > 0
        }

    fun listEducation(seekerId: Long): List<SeekerEducationDto> =
        transaction {
            SeekerEducationTable
                .selectAll()
                .where { SeekerEducationTable.seekerId eq seekerId }
                .map { it.toEducationDto() }
        }

    fun createEducation(seekerId: Long, request: CreateSeekerEducationRequest): SeekerEducationDto =
        transaction {
            val now = OffsetDateTime.now()
            val id =
                SeekerEducationTable.insert {
                    it[SeekerEducationTable.seekerId] = seekerId
                    it[institution] = request.institution.trim()
                    it[degree] = request.degree?.trim()?.ifBlank { null }
                    it[specialization] = request.specialization.trim()
                    it[endYear] = request.endYear
                    it[createdAt] = now
                    it[updatedAt] = now
                }[SeekerEducationTable.id].value
            SeekerEducationTable
                .selectAll()
                .where { SeekerEducationTable.id eq id }
                .first()
                .toEducationDto()
        }

    fun updateEducation(
        seekerId: Long,
        educationId: Long,
        request: UpdateSeekerEducationRequest,
    ): SeekerEducationDto? =
        transaction {
            val updated =
                SeekerEducationTable.update({
                    (SeekerEducationTable.id eq educationId) and (SeekerEducationTable.seekerId eq seekerId)
                }) {
                    it[institution] = request.institution.trim()
                    it[degree] = request.degree?.trim()?.ifBlank { null }
                    it[specialization] = request.specialization.trim()
                    it[endYear] = request.endYear
                    it[updatedAt] = OffsetDateTime.now()
                }
            if (updated == 0) return@transaction null
            SeekerEducationTable
                .selectAll()
                .where { SeekerEducationTable.id eq educationId }
                .first()
                .toEducationDto()
        }

    fun deleteEducation(seekerId: Long, educationId: Long): Boolean =
        transaction {
            SeekerEducationTable.deleteWhere {
                (SeekerEducationTable.id eq educationId) and (SeekerEducationTable.seekerId eq seekerId)
            } > 0
        }

    fun getSkillIds(seekerId: Long): List<Long> =
        transaction {
            SeekerSkillsTable
                .selectAll()
                .where { SeekerSkillsTable.seekerId eq seekerId }
                .map { it[SeekerSkillsTable.skillId].value }
        }

    fun setSkillIds(seekerId: Long, skillIds: List<Long>) {
        transaction {
            SeekerSkillsTable.deleteWhere { SeekerSkillsTable.seekerId eq seekerId }
            val now = OffsetDateTime.now()
            skillIds.distinct().forEach { skillId ->
                SeekerSkillsTable.insert {
                    it[SeekerSkillsTable.seekerId] = seekerId
                    it[SeekerSkillsTable.skillId] = skillId
                    it[createdAt] = now
                    it[updatedAt] = now
                }
            }
        }
    }

    fun getDesiredOccupationIds(seekerId: Long): List<Long> =
        transaction {
            SeekerDesiredPositionsTable
                .selectAll()
                .where { SeekerDesiredPositionsTable.seekerId eq seekerId }
                .map { it[SeekerDesiredPositionsTable.occupationId].value }
        }

    fun setDesiredOccupationIds(seekerId: Long, occupationIds: List<Long>) {
        transaction {
            SeekerDesiredPositionsTable.deleteWhere { SeekerDesiredPositionsTable.seekerId eq seekerId }
            val now = OffsetDateTime.now()
            occupationIds.distinct().forEach { occupationId ->
                SeekerDesiredPositionsTable.insert {
                    it[SeekerDesiredPositionsTable.seekerId] = seekerId
                    it[SeekerDesiredPositionsTable.occupationId] = occupationId
                    it[createdAt] = now
                }
            }
        }
    }

    private fun ResultRow.toDto() =
        SeekerProfileDto(
            id = this[SeekersTable.id].value,
            firstName = this[SeekersTable.firstName],
            middleName = this[SeekersTable.middleName],
            lastName = this[SeekersTable.lastName],
            phone = this[SeekersTable.phone],
            telegram = this[SeekersTable.telegram],
            linkedin = this[SeekersTable.linkedin],
        )

    private fun ResultRow.toExperienceDto() =
        SeekerExperienceDto(
            id = this[SeekerExperienceTable.id].value,
            companyName = this[SeekerExperienceTable.companyName],
            position = this[SeekerExperienceTable.position],
            description = this[SeekerExperienceTable.description],
            startDate = this[SeekerExperienceTable.startDate].toString(),
            endDate = this[SeekerExperienceTable.endDate]?.toString(),
        )

    private fun ResultRow.toEducationDto() =
        SeekerEducationDto(
            id = this[SeekerEducationTable.id].value,
            institution = this[SeekerEducationTable.institution],
            degree = this[SeekerEducationTable.degree],
            specialization = this[SeekerEducationTable.specialization],
            endYear = this[SeekerEducationTable.endYear],
        )
}
