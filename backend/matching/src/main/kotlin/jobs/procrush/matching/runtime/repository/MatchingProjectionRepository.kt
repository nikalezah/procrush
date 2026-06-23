package jobs.procrush.matching.runtime.repository

import jobs.procrush.matching.events.JobProfileChangedPayload
import jobs.procrush.matching.events.SeekerPersonalityReadyPayload
import jobs.procrush.matching.events.SeekerProfileChangedPayload
import jobs.procrush.matching.model.JobMatchCandidate
import jobs.procrush.matching.model.SeekerMatchCandidate
import jobs.procrush.matching.runtime.bootstrap.MatchingDatabaseRegistry
import jobs.procrush.matching.runtime.tables.JobProfileSnapshotsTable
import jobs.procrush.matching.runtime.tables.SeekerSnapshotsTable
import jobs.procrush.personality.dto.PersonalityAxesDto
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.OffsetDateTime

class MatchingProjectionRepository(
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    fun upsertSeeker(payload: SeekerProfileChangedPayload) {
        transaction(MatchingDatabaseRegistry.matching) {
            SeekerSnapshotsTable.deleteWhere { SeekerSnapshotsTable.seekerId eq payload.seekerId }
            SeekerSnapshotsTable.insert {
                it[seekerId] = payload.seekerId
                it[desiredOccupationIdsJson] = encodeLongList(payload.desiredOccupationIds)
                it[skillIdsJson] = encodeLongList(payload.skillIds)
                it[skillNamesJson] = json.encodeToString(ListSerializer(String.serializer()), payload.skillNames)
                it[personalityReady] = payload.personalityReady
                it[personalityAxesJson] = payload.personalityAxes?.let { axes -> PersonalityAxesDto.toJson(axes) }
                it[matchingEligible] = payload.matchingEligible
                it[firstName] = payload.firstName
                it[lastName] = payload.lastName
                it[updatedAt] = OffsetDateTime.now()
            }
        }
    }

    fun upsertSeeker(payload: SeekerPersonalityReadyPayload) {
        upsertSeeker(
            SeekerProfileChangedPayload(
                seekerId = payload.seekerId,
                desiredOccupationIds = payload.desiredOccupationIds,
                skillIds = payload.skillIds,
                personalityReady = true,
                personalityAxes = payload.personalityAxes,
                firstName = payload.firstName,
                lastName = payload.lastName,
                skillNames = payload.skillNames,
                matchingEligible = payload.matchingEligible,
            ),
        )
    }

    fun upsertJob(payload: JobProfileChangedPayload) {
        transaction(MatchingDatabaseRegistry.matching) {
            JobProfileSnapshotsTable.deleteWhere { JobProfileSnapshotsTable.jobProfileId eq payload.jobProfileId }
            if (payload.deleted) return@transaction
            JobProfileSnapshotsTable.insert {
                it[jobProfileId] = payload.jobProfileId
                it[occupationId] = payload.occupationId
                it[skillIdsJson] = encodeLongList(payload.skillIds)
                it[personalityAxesJson] = PersonalityAxesDto.toJson(payload.personalityAxes)
                it[isActive] = payload.isActive
                it[companyName] = payload.companyName
                it[occupationName] = payload.occupationName
                it[description] = payload.description
                it[updatedAt] = OffsetDateTime.now()
            }
        }
    }

    fun deleteJob(jobProfileId: Long) {
        transaction(MatchingDatabaseRegistry.matching) {
            JobProfileSnapshotsTable.deleteWhere { JobProfileSnapshotsTable.jobProfileId eq jobProfileId }
        }
    }

    fun findMatchableJobProfiles(occupationIds: List<Long>): List<JobMatchCandidate> {
        if (occupationIds.isEmpty()) return emptyList()
        return transaction(MatchingDatabaseRegistry.matching) {
            JobProfileSnapshotsTable
                .selectAll()
                .where {
                    (JobProfileSnapshotsTable.isActive eq true) and
                        (JobProfileSnapshotsTable.occupationId inList occupationIds)
                }
                .map { it.toJobMatchCandidate() }
        }
    }

    fun findMatchableSeekers(occupationId: Long): List<SeekerMatchCandidate> =
        transaction(MatchingDatabaseRegistry.matching) {
            SeekerSnapshotsTable
                .selectAll()
                .where { SeekerSnapshotsTable.matchingEligible eq true }
                .mapNotNull { row -> row.toSeekerMatchCandidate(occupationId) }
        }

    fun countEligibleSeekersForOccupations(occupationIds: List<Long>): Map<Long, Int> {
        if (occupationIds.isEmpty()) return emptyMap()
        return transaction(MatchingDatabaseRegistry.matching) {
            val counts = occupationIds.associateWith { 0 }.toMutableMap()
            SeekerSnapshotsTable
                .selectAll()
                .where { SeekerSnapshotsTable.matchingEligible eq true }
                .forEach { row ->
                    val desiredOccupationIds = decodeLongList(row[SeekerSnapshotsTable.desiredOccupationIdsJson])
                    desiredOccupationIds.forEach { occupationId ->
                        if (occupationId in counts) {
                            counts[occupationId] = counts.getValue(occupationId) + 1
                        }
                    }
                }
            counts
        }
    }

    private fun encodeLongList(values: List<Long>): String =
        json.encodeToString(ListSerializer(Long.serializer()), values)

    private fun decodeLongList(raw: String): List<Long> =
        runCatching {
            json.decodeFromString(ListSerializer(Long.serializer()), raw)
        }.getOrDefault(emptyList())

    private fun decodeSkillNames(raw: String): List<String> =
        runCatching {
            json.decodeFromString(ListSerializer(String.serializer()), raw)
        }.getOrDefault(emptyList())

    private fun ResultRow.toJobMatchCandidate(): JobMatchCandidate {
        val skillIds = decodeLongList(this[JobProfileSnapshotsTable.skillIdsJson]).toSet()
        val personalityAxes =
            PersonalityAxesDto.fromJson(this[JobProfileSnapshotsTable.personalityAxesJson])
        return JobMatchCandidate(
            jobProfileId = this[JobProfileSnapshotsTable.jobProfileId],
            employerId = 0,
            companyName = this[JobProfileSnapshotsTable.companyName],
            occupationId = this[JobProfileSnapshotsTable.occupationId],
            occupationName = this[JobProfileSnapshotsTable.occupationName],
            description = this[JobProfileSnapshotsTable.description],
            isActive = this[JobProfileSnapshotsTable.isActive],
            skillIds = skillIds,
            personalityAxes = personalityAxes,
        )
    }

    private fun ResultRow.toSeekerMatchCandidate(occupationId: Long): SeekerMatchCandidate? {
        val desiredOccupationIds = decodeLongList(this[SeekerSnapshotsTable.desiredOccupationIdsJson])
        if (occupationId !in desiredOccupationIds) return null

        val personalityReady = this[SeekerSnapshotsTable.personalityReady]
        val personalityAxes =
            this[SeekerSnapshotsTable.personalityAxesJson]
                ?.let { PersonalityAxesDto.fromJson(it) }
                ?.takeIf { personalityReady }

        return SeekerMatchCandidate(
            seekerId = this[SeekerSnapshotsTable.seekerId],
            firstName = this[SeekerSnapshotsTable.firstName],
            lastName = this[SeekerSnapshotsTable.lastName],
            occupationId = occupationId,
            occupationName = "—",
            skillIds = decodeLongList(this[SeekerSnapshotsTable.skillIdsJson]).toSet(),
            skillNames = decodeSkillNames(this[SeekerSnapshotsTable.skillNamesJson]),
            personalityAxes = personalityAxes,
            personalityReady = personalityReady,
        )
    }
}
