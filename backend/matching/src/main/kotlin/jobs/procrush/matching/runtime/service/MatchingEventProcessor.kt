package jobs.procrush.matching.runtime.service

import jobs.procrush.matching.events.JobProfileChangedPayload
import jobs.procrush.matching.events.MatchResultsUpdatedPayload
import jobs.procrush.matching.events.MatchScorePairDto
import jobs.procrush.matching.events.MatchingEventJson
import jobs.procrush.matching.events.MatchingEventTypes
import jobs.procrush.matching.events.SeekerPersonalityReadyPayload
import jobs.procrush.matching.events.SeekerProfileChangedPayload
import jobs.procrush.matching.model.JobMatchCandidate
import jobs.procrush.matching.model.SeekerMatchCandidate
import jobs.procrush.matching.runtime.model.StoredMatchResult
import jobs.procrush.matching.runtime.repository.MatchResultsRepository
import jobs.procrush.matching.runtime.repository.MatchingProjectionRepository
import jobs.procrush.matching.service.MatchScoringService
import kotlinx.serialization.json.Json
import java.time.OffsetDateTime

class MatchingEventProcessor(
    private val projectionRepository: MatchingProjectionRepository,
    private val matchResultsRepository: MatchResultsRepository,
    private val matchResultsEventPublisher: MatchResultsEventPublisher,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    fun processSeekerProfileChanged(payload: SeekerProfileChangedPayload) {
        projectionRepository.upsertSeeker(payload)
        if (payload.desiredOccupationIds.isEmpty()) {
            matchResultsRepository.deleteAllForSeeker(payload.seekerId)
            matchResultsEventPublisher.publish(
                MatchingEventTypes.MATCH_RESULTS_UPDATED,
                payload.seekerId.toString(),
                MatchingEventJson.json.encodeToJsonElement(
                    MatchResultsUpdatedPayload.serializer(),
                    MatchResultsUpdatedPayload(
                        seekerId = payload.seekerId,
                        pairs = listOf(),
                        computedAt = OffsetDateTime.now().toString()
                    )
                ),
            )
            return
        }
        val jobs = projectionRepository.findMatchableJobProfiles(payload.desiredOccupationIds)
        val results =
            jobs.mapNotNull { job ->
                scorePair(payload, job)?.takeIf { job.isActive }
            }
        matchResultsRepository.upsertAll(results)
        matchResultsRepository.deleteForSeekerExceptJobs(
            payload.seekerId,
            results.map { it.jobProfileId }.toSet(),
        )
        val resultPayload = MatchResultsUpdatedPayload(
            seekerId = payload.seekerId,
            pairs = results.map {
                MatchScorePairDto(
                    seekerId = it.seekerId,
                    jobProfileId = it.jobProfileId,
                    matchScore = it.matchScore,
                    matchScoreDisplay = it.matchScoreDisplay,
                    personalityIncluded = it.personalityIncluded,
                )
            },
            computedAt = results.maxOfOrNull { it.computedAt }?.toString() ?: OffsetDateTime.now().toString()
        )
        matchResultsEventPublisher.publish(
            MatchingEventTypes.MATCH_RESULTS_UPDATED,
            payload.seekerId.toString(),
            MatchingEventJson.json.encodeToJsonElement(MatchResultsUpdatedPayload.serializer(), resultPayload),
        )
    }

    fun processSeekerPersonalityReady(payload: SeekerPersonalityReadyPayload) {
        processSeekerProfileChanged(
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

    fun processJobProfileChanged(payload: JobProfileChangedPayload) {
        if (payload.deleted || !payload.isActive) {
            projectionRepository.deleteJob(payload.jobProfileId)
            matchResultsRepository.deleteAllForJob(payload.jobProfileId)
            matchResultsEventPublisher.publish(
                MatchingEventTypes.MATCH_RESULTS_UPDATED,
                payload.jobProfileId.toString(),
                MatchingEventJson.json.encodeToJsonElement(
                    MatchResultsUpdatedPayload.serializer(),
                    MatchResultsUpdatedPayload(
                        jobProfileId = payload.jobProfileId,
                        pairs = listOf(),
                        computedAt = OffsetDateTime.now().toString()
                    )
                ),
            )
            return
        }
        projectionRepository.upsertJob(payload)
        val job = payload.toJobCandidate()
        val seekers = projectionRepository.findMatchableSeekers(payload.occupationId)
        val results =
            seekers.mapNotNull { seeker ->
                scorePair(seeker, job)
            }
        matchResultsRepository.upsertAll(results)
        matchResultsRepository.deleteForJobExceptSeekers(
            payload.jobProfileId,
            results.map { it.seekerId }.toSet(),
        )
        val resultPayload = MatchResultsUpdatedPayload(
            jobProfileId = payload.jobProfileId,
            pairs = results.map {
                MatchScorePairDto(
                    seekerId = it.seekerId,
                    jobProfileId = it.jobProfileId,
                    matchScore = it.matchScore,
                    matchScoreDisplay = it.matchScoreDisplay,
                    personalityIncluded = it.personalityIncluded,
                )
            },
            computedAt = results.maxOfOrNull { it.computedAt }?.toString() ?: OffsetDateTime.now().toString()
        )
        matchResultsEventPublisher.publish(
            MatchingEventTypes.MATCH_RESULTS_UPDATED,
            payload.jobProfileId.toString(),
            MatchingEventJson.json.encodeToJsonElement(MatchResultsUpdatedPayload.serializer(), resultPayload),
        )
    }

    private fun scorePair(
        seeker: SeekerProfileChangedPayload,
        job: JobMatchCandidate,
    ): StoredMatchResult? {
        if (job.occupationId !in seeker.desiredOccupationIds) return null
        val skills = MatchScoringService.skillsScore(seeker.skillIds.toSet(), job.skillIds)
        val personalityAxes = seeker.personalityAxes
        val personality =
            if (seeker.personalityReady && personalityAxes != null) {
                MatchScoringService.personalityScore(personalityAxes, job.personalityAxes)
            } else {
                null
            }
        val matchScore =
            MatchScoringService.combinedScore(skills, personality, seeker.personalityReady)
        val personalityIncluded = seeker.personalityReady && personality != null
        return StoredMatchResult(
            seekerId = seeker.seekerId,
            jobProfileId = job.jobProfileId,
            occupationId = job.occupationId,
            companyName = job.companyName,
            positionName = job.occupationName,
            jobDescription = job.description.orEmpty(),
            seekerFirstName = seeker.firstName,
            seekerLastName = seeker.lastName,
            seekerSkillsJson = json.encodeToString(seeker.skillNames),
            matchScore = matchScore,
            matchScoreDisplay = MatchScoringService.toDisplayScore(matchScore),
            personalityIncluded = personalityIncluded,
            computedAt = OffsetDateTime.now(),
        )
    }

    private fun scorePair(
        seeker: SeekerMatchCandidate,
        job: JobMatchCandidate,
    ): StoredMatchResult? {
        val skills = MatchScoringService.skillsScore(seeker.skillIds, job.skillIds)
        val personalityAxes = seeker.personalityAxes
        val personality =
            if (seeker.personalityReady && personalityAxes != null) {
                MatchScoringService.personalityScore(personalityAxes, job.personalityAxes)
            } else {
                null
            }
        val matchScore =
            MatchScoringService.combinedScore(skills, personality, seeker.personalityReady)
        val personalityIncluded = seeker.personalityReady && personality != null
        return StoredMatchResult(
            seekerId = seeker.seekerId,
            jobProfileId = job.jobProfileId,
            occupationId = job.occupationId,
            companyName = job.companyName,
            positionName = job.occupationName,
            jobDescription = job.description.orEmpty(),
            seekerFirstName = seeker.firstName,
            seekerLastName = seeker.lastName,
            seekerSkillsJson = json.encodeToString(seeker.skillNames),
            matchScore = matchScore,
            matchScoreDisplay = MatchScoringService.toDisplayScore(matchScore),
            personalityIncluded = personalityIncluded,
            computedAt = OffsetDateTime.now(),
        )
    }

    private fun JobProfileChangedPayload.toJobCandidate(): JobMatchCandidate =
        JobMatchCandidate(
            jobProfileId = jobProfileId,
            employerId = 0,
            companyName = companyName.orEmpty(),
            occupationId = occupationId,
            occupationName = occupationName,
            description = description,
            isActive = isActive,
            skillIds = skillIds.toSet(),
            personalityAxes = personalityAxes,
        )
}
