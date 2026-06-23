package jobs.procrush.matching.runtime.tables

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object SeekerSnapshotsTable : Table("seeker_snapshots") {
    val seekerId = long("seeker_id")
    val desiredOccupationIdsJson = text("desired_occupation_ids_json")
    val skillIdsJson = text("skill_ids_json")
    val skillNamesJson = text("skill_names_json")
    val personalityReady = bool("personality_ready")
    val personalityAxesJson = text("personality_axes_json").nullable()
    val matchingEligible = bool("matching_eligible")
    val firstName = text("first_name")
    val lastName = text("last_name")
    val updatedAt = timestampWithTimeZone("updated_at")

    override val primaryKey = PrimaryKey(seekerId)
}
