package jobs.procrush.db

import jobs.procrush.db.tables.EmployerJobProfilesTable
import jobs.procrush.db.tables.EmployersTable
import jobs.procrush.db.tables.JobProfileSkillsTable
import jobs.procrush.db.tables.OccupationsTable
import jobs.procrush.db.tables.SeekerDesiredPositionsTable
import jobs.procrush.db.tables.SeekerPersonalProfilesTable
import jobs.procrush.db.tables.SeekerSkillsTable
import jobs.procrush.db.tables.SeekersTable
import jobs.procrush.db.tables.SurveyResultsTable
import jobs.procrush.db.tables.SurveysTable
import jobs.procrush.models.PersonalityAxesDto
import jobs.procrush.models.PersonalityProfileStatus
import jobs.procrush.survey.SurveyFlowRules
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

data class JobMatchCandidate(
    val jobProfileId: Long,
    val companyName: String,
    val occupationId: Long,
    val occupationName: String,
    val description: String?,
    val skillIds: Set<Long>,
    val personalityAxes: PersonalityAxesDto,
)

data class SeekerMatchCandidate(
    val seekerId: Long,
    val firstName: String,
    val lastName: String,
    val occupationId: Long,
    val occupationName: String,
    val skillIds: Set<Long>,
    val skillNames: List<String>,
    val personalityAxes: PersonalityAxesDto?,
    val personalityReady: Boolean,
)

data class SeekerMatchingContext(
    val skillIds: Set<Long>,
    val personalityAxes: PersonalityAxesDto?,
    val personalityReady: Boolean,
)

class MatchingRepository(
    private val referenceRepository: ReferenceRepository,
) {
    fun findMatchableJobProfiles(occupationIds: List<Long>): List<JobMatchCandidate> {
        if (occupationIds.isEmpty()) return emptyList()
        return transaction {
            EmployerJobProfilesTable
                .selectAll()
                .where {
                    (EmployerJobProfilesTable.isActive eq true) and
                        (EmployerJobProfilesTable.occupationId inList occupationIds)
                }
                .map { it.toJobMatchCandidate() }
        }
    }

    fun findMatchableSeekers(occupationId: Long): List<SeekerMatchCandidate> =
        transaction {
            val eligibleSeekerIds = findSeekerIdsWithAllTestsCompleteInTx()
            if (eligibleSeekerIds.isEmpty()) return@transaction emptyList()

            val seekerIdsForOccupation =
                SeekerDesiredPositionsTable
                    .selectAll()
                    .where {
                        (SeekerDesiredPositionsTable.occupationId eq occupationId) and
                            (SeekerDesiredPositionsTable.seekerId inList eligibleSeekerIds.toList())
                    }
                    .map { it[SeekerDesiredPositionsTable.seekerId].value }
                    .toSet()

            if (seekerIdsForOccupation.isEmpty()) return@transaction emptyList()

            val occupationName =
                OccupationsTable
                    .selectAll()
                    .where { OccupationsTable.id eq occupationId }
                    .firstOrNull()
                    ?.get(OccupationsTable.name)
                    ?: "—"

            seekerIdsForOccupation.mapNotNull { seekerId ->
                SeekersTable
                    .selectAll()
                    .where { SeekersTable.id eq seekerId }
                    .firstOrNull()
                    ?.toSeekerMatchCandidate(occupationId, occupationName)
            }
        }

    fun countMatchableSeekers(occupationId: Long): Int = findMatchableSeekers(occupationId).size

    fun getSeekerMatchingContext(seekerId: Long): SeekerMatchingContext? =
        transaction {
            val eligibleSeekerIds = findSeekerIdsWithAllTestsCompleteInTx()
            if (seekerId !in eligibleSeekerIds) return@transaction null

            val skillIds = getSeekerSkillIds(seekerId)
            val personalityRow =
                SeekerPersonalProfilesTable
                    .selectAll()
                    .where { SeekerPersonalProfilesTable.seekerId eq seekerId }
                    .firstOrNull()

            val status =
                personalityRow?.get(SeekerPersonalProfilesTable.generationStatus)?.let {
                    runCatching { PersonalityProfileStatus.valueOf(it) }.getOrNull()
                }
            val axesFromRow =
                personalityRow?.let { row ->
                    listOf(
                        row[SeekerPersonalProfilesTable.axisDominance]?.toDouble(),
                        row[SeekerPersonalProfilesTable.axisInfluence]?.toDouble(),
                        row[SeekerPersonalProfilesTable.axisStability]?.toDouble(),
                        row[SeekerPersonalProfilesTable.axisIntegrity]?.toDouble(),
                        row[SeekerPersonalProfilesTable.axisAutonomy]?.toDouble(),
                        row[SeekerPersonalProfilesTable.axisPace]?.toDouble(),
                    ).all { it != null }
                } == true
            val personalityReady = status == PersonalityProfileStatus.READY && axesFromRow
            val personalityAxes =
                if (personalityReady && personalityRow != null) {
                    PersonalityAxesDto(
                        axisDominance = personalityRow[SeekerPersonalProfilesTable.axisDominance]!!.toDouble(),
                        axisInfluence = personalityRow[SeekerPersonalProfilesTable.axisInfluence]!!.toDouble(),
                        axisStability = personalityRow[SeekerPersonalProfilesTable.axisStability]!!.toDouble(),
                        axisIntegrity = personalityRow[SeekerPersonalProfilesTable.axisIntegrity]!!.toDouble(),
                        axisAutonomy = personalityRow[SeekerPersonalProfilesTable.axisAutonomy]!!.toDouble(),
                        axisPace = personalityRow[SeekerPersonalProfilesTable.axisPace]!!.toDouble(),
                    )
                } else {
                    null
                }
            SeekerMatchingContext(
                skillIds = skillIds,
                personalityAxes = personalityAxes,
                personalityReady = personalityReady,
            )
        }

    fun findSeekerIdsWithAllTestsComplete(): Set<Long> =
        transaction { findSeekerIdsWithAllTestsCompleteInTx() }

    private fun findSeekerIdsWithAllTestsCompleteInTx(): Set<Long> {
            val requiredSurveyIds =
                SurveysTable
                    .selectAll()
                    .where {
                        SurveysTable.groupCode inList
                            listOf(SurveyFlowRules.CORE_GROUP, SurveyFlowRules.GROUP_64QN)
                    }
                    .map { it[SurveysTable.id].value }
                    .toSet()

            if (requiredSurveyIds.isEmpty()) return emptySet()

            val completedBySeeker =
                SurveyResultsTable
                    .selectAll()
                    .where { SurveyResultsTable.completedAt.isNotNull() }
                    .mapNotNull { row ->
                        val seekerId = row[SurveyResultsTable.seekerId]?.value ?: return@mapNotNull null
                        val surveyId = row[SurveyResultsTable.surveyId]?.value ?: return@mapNotNull null
                        seekerId to surveyId
                    }
                    .groupBy({ it.first }, { it.second })
                    .mapValues { (_, surveyIds) -> surveyIds.toSet() }

            return completedBySeeker
                .filter { (_, completedSurveyIds) -> requiredSurveyIds.all { it in completedSurveyIds } }
                .keys
    }

    private fun getJobProfileSkillIds(jobProfileId: Long): Set<Long> =
        JobProfileSkillsTable
            .selectAll()
            .where { JobProfileSkillsTable.jobProfileId eq jobProfileId }
            .map { it[JobProfileSkillsTable.skillId].value }
            .toSet()

    private fun getSeekerSkillIds(seekerId: Long): Set<Long> =
        SeekerSkillsTable
            .selectAll()
            .where { SeekerSkillsTable.seekerId eq seekerId }
            .map { it[SeekerSkillsTable.skillId].value }
            .toSet()

    private fun ResultRow.toJobMatchCandidate(): JobMatchCandidate {
        val jobProfileId = this[EmployerJobProfilesTable.id].value
        val occupationId = this[EmployerJobProfilesTable.occupationId].value
        val employerId = this[EmployerJobProfilesTable.employerId].value
        val companyName =
            EmployersTable
                .selectAll()
                .where { EmployersTable.id eq employerId }
                .firstOrNull()
                ?.get(EmployersTable.name)
                ?.ifBlank { "Компания не указана" }
                ?: "—"
        val occupationName =
            OccupationsTable
                .selectAll()
                .where { OccupationsTable.id eq occupationId }
                .firstOrNull()
                ?.get(OccupationsTable.name)
                ?: "—"

        return JobMatchCandidate(
            jobProfileId = jobProfileId,
            companyName = companyName,
            occupationId = occupationId,
            occupationName = occupationName,
            description = this[EmployerJobProfilesTable.description],
            skillIds = getJobProfileSkillIds(jobProfileId),
            personalityAxes = PersonalityAxesDto.fromJson(this[EmployerJobProfilesTable.requiredPersonality]),
        )
    }

    private fun ResultRow.toSeekerMatchCandidate(
        occupationId: Long,
        occupationName: String,
    ): SeekerMatchCandidate {
        val seekerId = this[SeekersTable.id].value
        val personalityRow =
            SeekerPersonalProfilesTable
                .selectAll()
                .where { SeekerPersonalProfilesTable.seekerId eq seekerId }
                .firstOrNull()

        val status =
            personalityRow?.get(SeekerPersonalProfilesTable.generationStatus)?.let {
                runCatching { PersonalityProfileStatus.valueOf(it) }.getOrNull()
            }
        val skillIds = getSeekerSkillIds(seekerId)
        val skillNames = referenceRepository.findSkillsByIds(skillIds.toList()).map { it.name }
        val axesFromRow =
            personalityRow?.let { row ->
                listOf(
                    row[SeekerPersonalProfilesTable.axisDominance]?.toDouble(),
                    row[SeekerPersonalProfilesTable.axisInfluence]?.toDouble(),
                    row[SeekerPersonalProfilesTable.axisStability]?.toDouble(),
                    row[SeekerPersonalProfilesTable.axisIntegrity]?.toDouble(),
                    row[SeekerPersonalProfilesTable.axisAutonomy]?.toDouble(),
                    row[SeekerPersonalProfilesTable.axisPace]?.toDouble(),
                ).all { it != null }
            } == true

        val personalityReady = status == PersonalityProfileStatus.READY && axesFromRow
        val personalityAxes =
            if (personalityReady && personalityRow != null) {
                PersonalityAxesDto(
                    axisDominance = personalityRow[SeekerPersonalProfilesTable.axisDominance]!!.toDouble(),
                    axisInfluence = personalityRow[SeekerPersonalProfilesTable.axisInfluence]!!.toDouble(),
                    axisStability = personalityRow[SeekerPersonalProfilesTable.axisStability]!!.toDouble(),
                    axisIntegrity = personalityRow[SeekerPersonalProfilesTable.axisIntegrity]!!.toDouble(),
                    axisAutonomy = personalityRow[SeekerPersonalProfilesTable.axisAutonomy]!!.toDouble(),
                    axisPace = personalityRow[SeekerPersonalProfilesTable.axisPace]!!.toDouble(),
                )
            } else {
                null
            }

        return SeekerMatchCandidate(
            seekerId = seekerId,
            firstName = this[SeekersTable.firstName],
            lastName = this[SeekersTable.lastName],
            occupationId = occupationId,
            occupationName = occupationName,
            skillIds = skillIds,
            skillNames = skillNames,
            personalityAxes = personalityAxes,
            personalityReady = personalityReady,
        )
    }
}
