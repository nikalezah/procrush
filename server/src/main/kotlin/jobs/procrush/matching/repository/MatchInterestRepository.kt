package jobs.procrush.matching.repository

import jobs.procrush.employer.tables.EmployersTable
import jobs.procrush.matching.dto.EmployerContactDto
import jobs.procrush.matching.dto.SeekerContactDto
import jobs.procrush.matching.model.MatchInterestRecord
import jobs.procrush.matching.tables.JobMatchInterestsTable
import jobs.procrush.seeker.tables.SeekersTable
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.time.OffsetDateTime

class MatchInterestRepository {
    fun recordSeekerResponse(seekerId: Long, jobProfileId: Long): MatchInterestRecord =
        transaction {
            val now = OffsetDateTime.now()
            val existing = findRecord(seekerId, jobProfileId)
            if (existing != null) {
                if (existing.seekerRespondedAt == null) {
                    JobMatchInterestsTable.update({
                        (JobMatchInterestsTable.seekerId eq seekerId) and
                            (JobMatchInterestsTable.jobProfileId eq jobProfileId)
                    }) {
                        it[seekerRespondedAt] = now
                    }
                }
                findRecord(seekerId, jobProfileId)!!
            } else {
                JobMatchInterestsTable.insert {
                    it[JobMatchInterestsTable.seekerId] = seekerId
                    it[JobMatchInterestsTable.jobProfileId] = jobProfileId
                    it[seekerRespondedAt] = now
                    it[employerRespondedAt] = null
                }
                findRecord(seekerId, jobProfileId)!!
            }
        }

    fun recordEmployerResponse(seekerId: Long, jobProfileId: Long): MatchInterestRecord =
        transaction {
            val now = OffsetDateTime.now()
            val existing = findRecord(seekerId, jobProfileId)
            if (existing != null) {
                if (existing.employerRespondedAt == null) {
                    JobMatchInterestsTable.update({
                        (JobMatchInterestsTable.seekerId eq seekerId) and
                            (JobMatchInterestsTable.jobProfileId eq jobProfileId)
                    }) {
                        it[employerRespondedAt] = now
                    }
                }
                findRecord(seekerId, jobProfileId)!!
            } else {
                JobMatchInterestsTable.insert {
                    it[JobMatchInterestsTable.seekerId] = seekerId
                    it[JobMatchInterestsTable.jobProfileId] = jobProfileId
                    it[seekerRespondedAt] = null
                    it[employerRespondedAt] = now
                }
                findRecord(seekerId, jobProfileId)!!
            }
        }

    fun findBySeekerAndJobProfiles(
        seekerId: Long,
        jobProfileIds: Collection<Long>,
    ): Map<Long, MatchInterestRecord> {
        if (jobProfileIds.isEmpty()) return emptyMap()
        return transaction {
            JobMatchInterestsTable
                .selectAll()
                .where {
                    (JobMatchInterestsTable.seekerId eq seekerId) and
                        (JobMatchInterestsTable.jobProfileId inList jobProfileIds.toList())
                }
                .associate { row ->
                    val record = row.toRecord()
                    record.jobProfileId to record
                }
        }
    }

    fun findByJobProfileAndSeekers(
        jobProfileId: Long,
        seekerIds: Collection<Long>,
    ): Map<Long, MatchInterestRecord> {
        if (seekerIds.isEmpty()) return emptyMap()
        return transaction {
            JobMatchInterestsTable
                .selectAll()
                .where {
                    (JobMatchInterestsTable.jobProfileId eq jobProfileId) and
                        (JobMatchInterestsTable.seekerId inList seekerIds.toList())
                }
                .associate { row ->
                    val record = row.toRecord()
                    record.seekerId to record
                }
        }
    }

    fun listBySeeker(seekerId: Long): List<MatchInterestRecord> =
        transaction {
            JobMatchInterestsTable
                .selectAll()
                .where {
                    (JobMatchInterestsTable.seekerId eq seekerId) and
                        (
                            JobMatchInterestsTable.seekerRespondedAt.isNotNull() or
                                JobMatchInterestsTable.employerRespondedAt.isNotNull()
                        )
                }
                .map { it.toRecord() }
        }

    fun listByJobProfile(jobProfileId: Long): List<MatchInterestRecord> =
        transaction {
            JobMatchInterestsTable
                .selectAll()
                .where {
                    (JobMatchInterestsTable.jobProfileId eq jobProfileId) and
                        (
                            JobMatchInterestsTable.seekerRespondedAt.isNotNull() or
                                JobMatchInterestsTable.employerRespondedAt.isNotNull()
                        )
                }
                .map { it.toRecord() }
        }

    fun findEmployerContact(employerId: Long): EmployerContactDto? =
        transaction {
            EmployersTable
                .selectAll()
                .where { EmployersTable.id eq employerId }
                .firstOrNull()
                ?.let { row ->
                    EmployerContactDto(
                        companyName = row[EmployersTable.name].ifBlank { "Компания не указана" },
                        phone = row[EmployersTable.phone],
                        emailContact = row[EmployersTable.emailContact],
                        website = row[EmployersTable.website],
                    )
                }
        }

    fun findSeekerContact(seekerId: Long): SeekerContactDto? =
        transaction {
            SeekersTable
                .selectAll()
                .where { SeekersTable.id eq seekerId }
                .firstOrNull()
                ?.let { row ->
                    SeekerContactDto(
                        firstName = row[SeekersTable.firstName],
                        lastName = row[SeekersTable.lastName],
                        phone = row[SeekersTable.phone],
                        telegram = row[SeekersTable.telegram],
                        linkedin = row[SeekersTable.linkedin],
                    )
                }
        }

    private fun findRecord(seekerId: Long, jobProfileId: Long): MatchInterestRecord? =
        JobMatchInterestsTable
            .selectAll()
            .where {
                (JobMatchInterestsTable.seekerId eq seekerId) and
                    (JobMatchInterestsTable.jobProfileId eq jobProfileId)
            }
            .firstOrNull()
            ?.toRecord()

    private fun ResultRow.toRecord() =
        MatchInterestRecord(
            seekerId = this[JobMatchInterestsTable.seekerId].value,
            jobProfileId = this[JobMatchInterestsTable.jobProfileId].value,
            seekerRespondedAt = this[JobMatchInterestsTable.seekerRespondedAt],
            employerRespondedAt = this[JobMatchInterestsTable.employerRespondedAt],
        )
}
