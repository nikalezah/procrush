package jobs.procrush.matching.repository

import jobs.procrush.matching.events.MatchResultsUpdatedPayload
import jobs.procrush.matching.events.MatchScorePairDto
import jobs.procrush.matching.model.MatchScoreApplyResult
import jobs.procrush.matching.model.StoredMatchScore
import jobs.procrush.matching.tables.MatchScoresTable
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.notInList
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.OffsetDateTime

class MatchScoreRepository {
    fun applyResults(payload: MatchResultsUpdatedPayload): MatchScoreApplyResult =
        transaction {
            val computedAt =
                runCatching { OffsetDateTime.parse(payload.computedAt) }
                    .getOrElse { OffsetDateTime.now() }
            val affectedSeekers = mutableSetOf<Long>()
            val affectedJobs = mutableSetOf<Long>()
            val seekerId = payload.seekerId
            val jobProfileId = payload.jobProfileId

            when {
                seekerId != null -> {
                    val previousJobs =
                        MatchScoresTable
                            .selectAll()
                            .where { MatchScoresTable.seekerId eq seekerId }
                            .map { it[MatchScoresTable.jobProfileId] }
                    replaceForSeeker(seekerId, payload.pairs, computedAt)
                    affectedSeekers.add(seekerId)
                    affectedJobs.addAll(previousJobs)
                    affectedJobs.addAll(payload.pairs.map { it.jobProfileId })
                }
                jobProfileId != null -> {
                    val previousSeekers =
                        MatchScoresTable
                            .selectAll()
                            .where { MatchScoresTable.jobProfileId eq jobProfileId }
                            .map { it[MatchScoresTable.seekerId] }
                    replaceForJob(jobProfileId, payload.pairs, computedAt)
                    affectedJobs.add(jobProfileId)
                    affectedSeekers.addAll(previousSeekers)
                    affectedSeekers.addAll(payload.pairs.map { it.seekerId })
                }
                else -> error("match.results_updated requires seekerId or jobProfileId")
            }

            MatchScoreApplyResult(
                affectedSeekerIds = affectedSeekers,
                affectedJobProfileIds = affectedJobs,
                computedAt = computedAt,
            )
        }

    fun listForSeeker(seekerId: Long): List<StoredMatchScore> =
        transaction {
            MatchScoresTable
                .selectAll()
                .where { MatchScoresTable.seekerId eq seekerId }
                .orderBy(MatchScoresTable.matchScore to SortOrder.DESC)
                .map { it.toStored() }
        }

    fun listForJob(jobProfileId: Long): List<StoredMatchScore> =
        transaction {
            MatchScoresTable
                .selectAll()
                .where { MatchScoresTable.jobProfileId eq jobProfileId }
                .orderBy(MatchScoresTable.matchScore to SortOrder.DESC)
                .map { it.toStored() }
        }

    fun findPair(seekerId: Long, jobProfileId: Long): StoredMatchScore? =
        transaction {
            MatchScoresTable
                .selectAll()
                .where {
                    (MatchScoresTable.seekerId eq seekerId) and
                        (MatchScoresTable.jobProfileId eq jobProfileId)
                }
                .firstOrNull()
                ?.toStored()
        }

    private fun replaceForSeeker(
        seekerId: Long,
        pairs: List<MatchScorePairDto>,
        computedAt: OffsetDateTime,
    ) {
        val keepIds = pairs.map { it.jobProfileId }.toSet()
        pairs.forEach { pair ->
            upsertPair(pair, computedAt)
        }
        if (keepIds.isEmpty()) {
            MatchScoresTable.deleteWhere { MatchScoresTable.seekerId eq seekerId }
        } else {
            MatchScoresTable.deleteWhere {
                (MatchScoresTable.seekerId eq seekerId) and
                    (MatchScoresTable.jobProfileId notInList keepIds.toList())
            }
        }
    }

    private fun replaceForJob(
        jobProfileId: Long,
        pairs: List<MatchScorePairDto>,
        computedAt: OffsetDateTime,
    ) {
        val keepIds = pairs.map { it.seekerId }.toSet()
        pairs.forEach { pair ->
            upsertPair(pair, computedAt)
        }
        if (keepIds.isEmpty()) {
            MatchScoresTable.deleteWhere { MatchScoresTable.jobProfileId eq jobProfileId }
        } else {
            MatchScoresTable.deleteWhere {
                (MatchScoresTable.jobProfileId eq jobProfileId) and
                    (MatchScoresTable.seekerId notInList keepIds.toList())
            }
        }
    }

    private fun upsertPair(pair: MatchScorePairDto, computedAt: OffsetDateTime) {
        MatchScoresTable.deleteWhere {
            (MatchScoresTable.seekerId eq pair.seekerId) and
                (MatchScoresTable.jobProfileId eq pair.jobProfileId)
        }
        MatchScoresTable.insert {
            it[seekerId] = pair.seekerId
            it[jobProfileId] = pair.jobProfileId
            it[matchScore] = pair.matchScore
            it[matchScoreDisplay] = pair.matchScoreDisplay
            it[personalityIncluded] = pair.personalityIncluded
            it[MatchScoresTable.computedAt] = computedAt
        }
    }

    private fun org.jetbrains.exposed.v1.core.ResultRow.toStored(): StoredMatchScore =
        StoredMatchScore(
            seekerId = this[MatchScoresTable.seekerId],
            jobProfileId = this[MatchScoresTable.jobProfileId],
            matchScore = this[MatchScoresTable.matchScore],
            matchScoreDisplay = this[MatchScoresTable.matchScoreDisplay],
            personalityIncluded = this[MatchScoresTable.personalityIncluded],
            computedAt = this[MatchScoresTable.computedAt],
        )
}
