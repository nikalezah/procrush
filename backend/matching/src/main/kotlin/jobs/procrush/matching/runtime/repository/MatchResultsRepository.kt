package jobs.procrush.matching.runtime.repository

import jobs.procrush.matching.runtime.bootstrap.MatchingDatabaseRegistry
import jobs.procrush.matching.runtime.model.StoredMatchResult
import jobs.procrush.matching.runtime.tables.MatchResultsTable
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.notInList
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class MatchResultsRepository {
    fun upsert(result: StoredMatchResult) {
        transaction(MatchingDatabaseRegistry.matching) {
            MatchResultsTable.deleteWhere {
                (MatchResultsTable.seekerId eq result.seekerId) and
                    (MatchResultsTable.jobProfileId eq result.jobProfileId)
            }
            insertRow(result)
        }
    }

    fun upsertAll(results: List<StoredMatchResult>) {
        if (results.isEmpty()) return
        transaction(MatchingDatabaseRegistry.matching) {
            results.forEach { result ->
                MatchResultsTable.deleteWhere {
                    (MatchResultsTable.seekerId eq result.seekerId) and
                        (MatchResultsTable.jobProfileId eq result.jobProfileId)
                }
                insertRow(result)
            }
        }
    }

    fun deleteForSeekerExceptJobs(seekerId: Long, keepJobProfileIds: Set<Long>) {
        transaction(MatchingDatabaseRegistry.matching) {
            if (keepJobProfileIds.isEmpty()) {
                MatchResultsTable.deleteWhere { MatchResultsTable.seekerId eq seekerId }
            } else {
                MatchResultsTable.deleteWhere {
                    (MatchResultsTable.seekerId eq seekerId) and
                        (MatchResultsTable.jobProfileId notInList keepJobProfileIds.toList())
                }
            }
        }
    }

    fun deleteForJobExceptSeekers(jobProfileId: Long, keepSeekerIds: Set<Long>) {
        transaction(MatchingDatabaseRegistry.matching) {
            if (keepSeekerIds.isEmpty()) {
                MatchResultsTable.deleteWhere { MatchResultsTable.jobProfileId eq jobProfileId }
            } else {
                MatchResultsTable.deleteWhere {
                    (MatchResultsTable.jobProfileId eq jobProfileId) and
                        (MatchResultsTable.seekerId notInList keepSeekerIds.toList())
                }
            }
        }
    }

    fun deleteAllForJob(jobProfileId: Long) {
        transaction(MatchingDatabaseRegistry.matching) {
            MatchResultsTable.deleteWhere { MatchResultsTable.jobProfileId eq jobProfileId }
        }
    }

    fun deleteAllForSeeker(seekerId: Long) {
        transaction(MatchingDatabaseRegistry.matching) {
            MatchResultsTable.deleteWhere { MatchResultsTable.seekerId eq seekerId }
        }
    }

    fun listForSeeker(seekerId: Long): List<StoredMatchResult> =
        transaction(MatchingDatabaseRegistry.matching) {
            MatchResultsTable
                .selectAll()
                .where { MatchResultsTable.seekerId eq seekerId }
                .orderBy(MatchResultsTable.matchScore to SortOrder.DESC)
                .map { it.toStored() }
        }

    fun listForJob(jobProfileId: Long): List<StoredMatchResult> =
        transaction(MatchingDatabaseRegistry.matching) {
            MatchResultsTable
                .selectAll()
                .where { MatchResultsTable.jobProfileId eq jobProfileId }
                .orderBy(MatchResultsTable.matchScore to SortOrder.DESC)
                .map { it.toStored() }
        }

    fun findPair(seekerId: Long, jobProfileId: Long): StoredMatchResult? =
        transaction(MatchingDatabaseRegistry.matching) {
            MatchResultsTable
                .selectAll()
                .where {
                    (MatchResultsTable.seekerId eq seekerId) and
                        (MatchResultsTable.jobProfileId eq jobProfileId)
                }
                .firstOrNull()
                ?.toStored()
        }

    fun countDistinctSeekersForOccupation(occupationId: Long): Int =
        transaction(MatchingDatabaseRegistry.matching) {
            MatchResultsTable
                .selectAll()
                .where { MatchResultsTable.occupationId eq occupationId }
                .map { it[MatchResultsTable.seekerId] }
                .distinct()
                .size
        }

    fun countDistinctSeekersForOccupations(occupationIds: List<Long>): Map<Long, Int> {
        if (occupationIds.isEmpty()) return emptyMap()
        return transaction(MatchingDatabaseRegistry.matching) {
            val grouped =
                MatchResultsTable
                    .selectAll()
                    .where { MatchResultsTable.occupationId inList occupationIds }
                    .groupBy { it[MatchResultsTable.occupationId] }
                    .mapValues { (_, rows) -> rows.map { it[MatchResultsTable.seekerId] }.distinct().size }
            occupationIds.associateWith { grouped[it] ?: 0 }
        }
    }

    private fun insertRow(result: StoredMatchResult) {
        MatchResultsTable.insert {
            it[MatchResultsTable.seekerId] = result.seekerId
            it[MatchResultsTable.jobProfileId] = result.jobProfileId
            it[MatchResultsTable.occupationId] = result.occupationId
            it[MatchResultsTable.companyName] = result.companyName
            it[MatchResultsTable.positionName] = result.positionName
            it[MatchResultsTable.jobDescription] = result.jobDescription
            it[MatchResultsTable.seekerFirstName] = result.seekerFirstName
            it[MatchResultsTable.seekerLastName] = result.seekerLastName
            it[MatchResultsTable.seekerSkillsJson] = result.seekerSkillsJson
            it[MatchResultsTable.matchScore] = result.matchScore
            it[MatchResultsTable.matchScoreDisplay] = result.matchScoreDisplay
            it[MatchResultsTable.personalityIncluded] = result.personalityIncluded
            it[MatchResultsTable.computedAt] = result.computedAt
        }
    }

    private fun org.jetbrains.exposed.v1.core.ResultRow.toStored(): StoredMatchResult =
        StoredMatchResult(
            seekerId = this[MatchResultsTable.seekerId],
            jobProfileId = this[MatchResultsTable.jobProfileId],
            occupationId = this[MatchResultsTable.occupationId],
            companyName = this[MatchResultsTable.companyName],
            positionName = this[MatchResultsTable.positionName],
            jobDescription = this[MatchResultsTable.jobDescription],
            seekerFirstName = this[MatchResultsTable.seekerFirstName],
            seekerLastName = this[MatchResultsTable.seekerLastName],
            seekerSkillsJson = this[MatchResultsTable.seekerSkillsJson],
            matchScore = this[MatchResultsTable.matchScore],
            matchScoreDisplay = this[MatchResultsTable.matchScoreDisplay],
            personalityIncluded = this[MatchResultsTable.personalityIncluded],
            computedAt = this[MatchResultsTable.computedAt],
        )
}
