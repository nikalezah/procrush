package jobs.procrush.matching.kafka

import jobs.procrush.employer.dto.JobProfileDto
import jobs.procrush.matching.events.JobProfileChangedPayload
import jobs.procrush.matching.events.MatchingEventJson
import jobs.procrush.matching.events.MatchingEventTypes
import jobs.procrush.matching.events.SeekerPersonalityReadyPayload
import jobs.procrush.matching.events.SeekerProfileChangedPayload
import jobs.procrush.matching.repository.MatchingRepository
import jobs.procrush.personality.dto.PersonalityAxesDto
import jobs.procrush.seeker.repository.SeekerRepository

class MatchingEventPayloadFactory(
    private val seekerRepository: SeekerRepository,
    private val matchingRepository: MatchingRepository,
) {
    fun publishSeekerProfileChanged(
        publisher: MatchingEventPublisher,
        seekerId: Long,
    ) {
        val payload = buildSeekerProfileChangedPayload(seekerId) ?: return
        publisher.publish(
            eventType = MatchingEventTypes.SEEKER_PROFILE_CHANGED,
            partitionKey = seekerId.toString(),
            payload = MatchingEventJson.json.encodeToJsonElement(SeekerProfileChangedPayload.serializer(), payload),
        )
    }

    fun publishSeekerPersonalityReady(
        publisher: MatchingEventPublisher,
        seekerId: Long,
        personalityAxes: PersonalityAxesDto,
    ) {
        val base = buildSeekerProfileChangedPayload(seekerId) ?: return
        val payload =
            SeekerPersonalityReadyPayload(
                seekerId = seekerId,
                desiredOccupationIds = base.desiredOccupationIds,
                skillIds = base.skillIds,
                personalityAxes = personalityAxes,
                matchingEligible = base.matchingEligible,
            )
        publisher.publish(
            eventType = MatchingEventTypes.SEEKER_PERSONALITY_READY,
            partitionKey = seekerId.toString(),
            payload = MatchingEventJson.json.encodeToJsonElement(SeekerPersonalityReadyPayload.serializer(), payload),
        )
    }

    fun publishJobProfileChanged(
        publisher: MatchingEventPublisher,
        jobProfile: JobProfileDto,
        @Suppress("UNUSED_PARAMETER") employerId: Long,
        deleted: Boolean = false,
    ) {
        val payload =
            JobProfileChangedPayload(
                jobProfileId = jobProfile.id,
                occupationId = jobProfile.occupationId,
                skillIds = jobProfile.skillIds,
                personalityAxes = jobProfile.personalityAxes,
                isActive = jobProfile.isActive,
                deleted = deleted,
            )
        publisher.publish(
            eventType = MatchingEventTypes.JOB_PROFILE_CHANGED,
            partitionKey = jobProfile.id.toString(),
            payload = MatchingEventJson.json.encodeToJsonElement(JobProfileChangedPayload.serializer(), payload),
        )
    }

    fun buildSeekerProfileChangedPayload(seekerId: Long): SeekerProfileChangedPayload? {
        if (seekerRepository.findById(seekerId) == null) return null
        val desiredOccupationIds = seekerRepository.getDesiredOccupationIds(seekerId)
        val skillIds = seekerRepository.getSkillIds(seekerId)
        val context = matchingRepository.getSeekerMatchingContext(seekerId)
        val matchingEligible = matchingRepository.findSeekerIdsWithAllTestsComplete().contains(seekerId)
        return SeekerProfileChangedPayload(
            seekerId = seekerId,
            desiredOccupationIds = desiredOccupationIds,
            skillIds = skillIds,
            personalityReady = context?.personalityReady == true,
            personalityAxes = context?.personalityAxes,
            matchingEligible = matchingEligible,
        )
    }
}
