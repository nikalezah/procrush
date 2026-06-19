package jobs.procrush.matching.tables

import jobs.procrush.employer.tables.EmployerJobProfilesTable
import jobs.procrush.seeker.tables.SeekersTable
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object JobMatchInterestsTable : LongIdTable("job_match_interests") {
    val seekerId = reference("seeker_id", SeekersTable, onDelete = ReferenceOption.CASCADE)
    val jobProfileId = reference("job_profile_id", EmployerJobProfilesTable, onDelete = ReferenceOption.CASCADE)
    val seekerRespondedAt = timestampWithTimeZone("seeker_responded_at").nullable()
    val employerRespondedAt = timestampWithTimeZone("employer_responded_at").nullable()

    init {
        uniqueIndex(seekerId, jobProfileId)
    }
}
