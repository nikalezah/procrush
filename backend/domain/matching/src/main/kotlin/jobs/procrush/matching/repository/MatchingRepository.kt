package jobs.procrush.matching.repository

import jobs.procrush.employer.tables.EmployerJobProfilesTable
import jobs.procrush.employer.tables.EmployersTable
import jobs.procrush.employer.tables.JobProfileSkillsTable
import jobs.procrush.matching.dto.apiCompanyName
import jobs.procrush.matching.model.JobMatchCandidate
import jobs.procrush.matching.model.SeekerMatchingContext
import jobs.procrush.personality.dto.PersonalityAxesDto
import jobs.procrush.personality.dto.PersonalityProfileStatus
import jobs.procrush.seeker.tables.SeekerPersonalProfilesTable
import jobs.procrush.seeker.tables.SeekerSkillsTable
import jobs.procrush.shared.repository.ReferenceRepository
import jobs.procrush.shared.tables.OccupationsTable
import jobs.procrush.survey.scoring.SurveyFlowRules
import jobs.procrush.survey.tables.SurveyResultsTable
import jobs.procrush.survey.tables.SurveysTable
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class MatchingRepository(
    private val referenceRepository: ReferenceRepository,
    private val database: Database? = null,
) {
    private inline fun <T> inTx(crossinline statement: () -> T): T =
        when (database) {
            null -> transaction { statement() }
            else -> transaction(database) { statement() }
        }

    fun findJobProfileById(jobProfileId: Long): JobMatchCandidate? =
        inTx {
            EmployerJobProfilesTable
                .selectAll()
                .where { EmployerJobProfilesTable.id eq jobProfileId }
                .firstOrNull()
                ?.toJobMatchCandidate()
        }

    fun getSeekerMatchingContext(
        seekerId: Long,
        testsAlreadyComplete: Boolean = false,
    ): SeekerMatchingContext? =
        inTx {
            if (!testsAlreadyComplete) {
                val eligibleSeekerIds = findSeekerIdsWithAllTestsCompleteInTx()
                if (seekerId !in eligibleSeekerIds) {
                    null
                } else {
                    buildContextForSeeker(seekerId)
                }
            } else {
                buildContextForSeeker(seekerId)
            }
        }

    private fun buildContextForSeeker(seekerId: Long): SeekerMatchingContext {
        val skillIds = getSeekerSkillIds(seekerId)
        val personalityRow =
            SeekerPersonalProfilesTable
                .selectAll()
                .where { SeekerPersonalProfilesTable.seekerId eq seekerId }
                .firstOrNull()

        return buildSeekerMatchingContext(skillIds, personalityRow)
    }

    fun findSeekerIdsWithAllTestsComplete(): Set<Long> =
        inTx { findSeekerIdsWithAllTestsCompleteInTx() }

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
                .where {
                    SurveyResultsTable.completedAt.isNotNull() and
                        (SurveyResultsTable.surveyId inList requiredSurveyIds.toList())
                }
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

    private fun buildSeekerMatchingContext(
        skillIds: Set<Long>,
        personalityRow: ResultRow?,
    ): SeekerMatchingContext {
        val personality = resolvePersonality(personalityRow)
        return SeekerMatchingContext(
            skillIds = skillIds,
            personalityAxes = personality.first,
            personalityReady = personality.second,
        )
    }

    private fun resolvePersonality(personalityRow: ResultRow?): Pair<PersonalityAxesDto?, Boolean> {
        if (personalityRow == null) return null to false

        val status =
            personalityRow[SeekerPersonalProfilesTable.generationStatus]?.let {
                runCatching { PersonalityProfileStatus.valueOf(it) }.getOrNull()
            }
        val axesFromRow =
            listOf(
                personalityRow[SeekerPersonalProfilesTable.axisDominance]?.toDouble(),
                personalityRow[SeekerPersonalProfilesTable.axisInfluence]?.toDouble(),
                personalityRow[SeekerPersonalProfilesTable.axisStability]?.toDouble(),
                personalityRow[SeekerPersonalProfilesTable.axisIntegrity]?.toDouble(),
                personalityRow[SeekerPersonalProfilesTable.axisAutonomy]?.toDouble(),
                personalityRow[SeekerPersonalProfilesTable.axisPace]?.toDouble(),
            ).all { it != null }

        val personalityReady = status == PersonalityProfileStatus.READY && axesFromRow
        val personalityAxes =
            if (personalityReady) {
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

        return personalityAxes to personalityReady
    }

    private fun resolveOccupationName(occupationId: Long): String =
        OccupationsTable
            .selectAll()
            .where { OccupationsTable.id eq occupationId }
            .firstOrNull()
            ?.get(OccupationsTable.name)
            ?: "—"

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
            apiCompanyName(
                EmployersTable
                    .selectAll()
                    .where { EmployersTable.id eq employerId }
                    .firstOrNull()
                    ?.get(EmployersTable.name),
            ).orEmpty()
        val occupationName = resolveOccupationName(occupationId)

        return JobMatchCandidate(
            jobProfileId = jobProfileId,
            employerId = employerId,
            companyName = companyName,
            occupationId = occupationId,
            occupationName = occupationName,
            description = this[EmployerJobProfilesTable.description],
            isActive = this[EmployerJobProfilesTable.isActive],
            skillIds = getJobProfileSkillIds(jobProfileId),
            personalityAxes = PersonalityAxesDto.fromJson(this[EmployerJobProfilesTable.requiredPersonality]),
        )
    }
}
