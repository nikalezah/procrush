package jobs.procrush.matching.runtime.tables

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object JobProfileSnapshotsTable : Table("job_profile_snapshots") {
    val jobProfileId = long("job_profile_id")
    val occupationId = long("occupation_id")
    val skillIdsJson = text("skill_ids_json")
    val personalityAxesJson = text("personality_axes_json")
    val isActive = bool("is_active")
    val updatedAt = timestampWithTimeZone("updated_at")

    override val primaryKey = PrimaryKey(jobProfileId)
}
