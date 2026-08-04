package jobs.procrush.matching.runtime.service

import jobs.procrush.bootstrap.kafka.KafkaStringPublisher
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
import jobs.procrush.observability.AppMetrics
import jobs.procrush.observability.MdcContext
import jobs.procrush.observability.TracePropagation
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime

class MatchingEventProcessor(
    private val projectionRepository: MatchingProjectionRepository,
    private val matchResultsRepository: MatchResultsRepository,
    private val resultsPublisher: KafkaStringPublisher,
) {
    private val logger = LoggerFactory.getLogger(MatchingEventProcessor::class.java)

    fun processSeekerProfileChanged(payload: SeekerProfileChangedPayload) {
        projectionRepository.upsertSeeker(payload)
        if (payload.desiredOccupationIds.isEmpty()) {
            matchResultsRepository.deleteAllForSeeker(payload.seekerId)
            publishResults(seekerId = payload.seekerId, results = emptyList())
            return
        }
        val jobs = projectionRepository.findMatchableJobProfiles(payload.desiredOccupationIds)
        val results =
            jobs.mapNotNull { job ->
                scorePair(payload, job)?.takeIf { job.isActive && it.matchScore > 0.0 }
            }
        matchResultsRepository.upsertAll(results)
        matchResultsRepository.deleteForSeekerExceptJobs(
            payload.seekerId,
            results.map { it.jobProfileId }.toSet(),
        )
        publishResults(seekerId = payload.seekerId, results = results)
    }

    fun processSeekerPersonalityReady(payload: SeekerPersonalityReadyPayload) {
        processSeekerProfileChanged(
            SeekerProfileChangedPayload(
                seekerId = payload.seekerId,
                desiredOccupationIds = payload.desiredOccupationIds,
                skillIds = payload.skillIds,
                personalityReady = true,
                personalityAxes = payload.personalityAxes,
                matchingEligible = payload.matchingEligible,
            ),
        )
    }

    fun processJobProfileChanged(payload: JobProfileChangedPayload) {
        if (payload.deleted || !payload.isActive) {
            projectionRepository.deleteJob(payload.jobProfileId)
            matchResultsRepository.deleteAllForJob(payload.jobProfileId)
            publishResults(jobProfileId = payload.jobProfileId, results = emptyList())
            return
        }
        projectionRepository.upsertJob(payload)
        val job = payload.toJobCandidate()
        val seekers = projectionRepository.findMatchableSeekers(payload.occupationId)
        val results =
            seekers.mapNotNull { seeker ->
                scorePair(seeker, job)?.takeIf { it.matchScore > 0.0 }
            }
        matchResultsRepository.upsertAll(results)
        matchResultsRepository.deleteForJobExceptSeekers(
            payload.jobProfileId,
            results.map { it.seekerId }.toSet(),
        )
        publishResults(jobProfileId = payload.jobProfileId, results = results)
    }

    private fun publishResults(
        seekerId: Long? = null,
        jobProfileId: Long? = null,
        results: List<StoredMatchResult>,
    ) {
        val partitionKey =
            (seekerId ?: jobProfileId)?.toString()
                ?: error("publishResults requires seekerId or jobProfileId")
        val correlationId = MdcContext.currentRequestId()
        val payload =
            MatchResultsUpdatedPayload(
                seekerId = seekerId,
                jobProfileId = jobProfileId,
                pairs = results.map { it.toScorePair() },
                computedAt = results.maxOfOrNull { it.computedAt }?.toString()
                    ?: OffsetDateTime.now().toString(),
            )
        val body =
            MatchingEventJson.encodeEnvelope(
                eventType = MatchingEventTypes.MATCH_RESULTS_UPDATED,
                payload = MatchingEventJson.json.encodeToJsonElement(
                    MatchResultsUpdatedPayload.serializer(),
                    payload,
                ),
                correlationId = correlationId,
            )
        resultsPublisher.publish(
            key = partitionKey,
            body = body,
            configure = { TracePropagation.injectCurrent(it) },
        ) { metadata, error ->
            if (error != null) {
                AppMetrics.kafkaPublishFailure()
                logger.error(
                    "Failed to publish match.results_updated key={} correlationId={}",
                    partitionKey,
                    correlationId,
                    error,
                )
            } else {
                logger.info(
                    "Published match.results_updated key={} correlationId={} partition={} offset={}",
                    partitionKey,
                    correlationId,
                    metadata?.partition(),
                    metadata?.offset(),
                )
            }
        }
    }

    private fun StoredMatchResult.toScorePair(): MatchScorePairDto =
        MatchScorePairDto(
            seekerId = seekerId,
            jobProfileId = jobProfileId,
            matchScore = matchScore,
            personalityIncluded = personalityIncluded,
        )

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
            matchScore = matchScore,
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
            matchScore = matchScore,
            personalityIncluded = personalityIncluded,
            computedAt = OffsetDateTime.now(),
        )
    }

    private fun JobProfileChangedPayload.toJobCandidate(): JobMatchCandidate =
        JobMatchCandidate(
            jobProfileId = jobProfileId,
            employerId = 0,
            companyName = "",
            occupationId = occupationId,
            occupationName = "",
            description = null,
            isActive = isActive,
            skillIds = skillIds.toSet(),
            personalityAxes = personalityAxes,
        )
}
