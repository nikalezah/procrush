package jobs.procrush.matching.port

import jobs.procrush.employer.dto.JobProfileDto
import jobs.procrush.personality.dto.PersonalityAxesDto

interface MatchingEventPort {
    fun publishSeekerProfileChanged(seekerId: Long)

    fun publishSeekerPersonalityReady(seekerId: Long, personalityAxes: PersonalityAxesDto)

    fun publishJobProfileChanged(
        jobProfile: JobProfileDto,
        employerId: Long,
        deleted: Boolean = false,
    )
}
