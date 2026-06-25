package jobs.procrush.composition

import jobs.procrush.employer.dto.JobProfileDto
import jobs.procrush.matching.kafka.MatchingEventPayloadFactory
import jobs.procrush.matching.kafka.MatchingEventPublisher
import jobs.procrush.matching.port.MatchingEventPort
import jobs.procrush.personality.dto.PersonalityAxesDto

class MatchingEventPortAdapter(
    private val publisher: MatchingEventPublisher,
    private val payloadFactory: MatchingEventPayloadFactory,
) : MatchingEventPort {
    override fun publishSeekerProfileChanged(seekerId: Long) {
        payloadFactory.publishSeekerProfileChanged(publisher, seekerId)
    }

    override fun publishSeekerPersonalityReady(seekerId: Long, personalityAxes: PersonalityAxesDto) {
        payloadFactory.publishSeekerPersonalityReady(publisher, seekerId, personalityAxes)
    }

    override fun publishJobProfileChanged(
        jobProfile: JobProfileDto,
        employerId: Long,
        deleted: Boolean,
    ) {
        payloadFactory.publishJobProfileChanged(publisher, jobProfile, employerId, deleted)
    }
}
