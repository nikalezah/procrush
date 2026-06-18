package jobs.procrush.seeker.tables

import jobs.procrush.shared.tables.SuperpowersAndTalentsTable
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object SeekerSuperpowersAndTalentsTable : Table("seeker_superpowers_and_talents") {
    val seekerPersonalProfileId =
        reference("seeker_personal_profile_id", SeekersTable, onDelete = ReferenceOption.CASCADE)
    val superpowersAndTalentsId =
        reference("superpowers_and_talents_id", SuperpowersAndTalentsTable, onDelete = ReferenceOption.CASCADE)
    val isPronounced = bool("is_pronounced").nullable()
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")

    override val primaryKey = PrimaryKey(seekerPersonalProfileId, superpowersAndTalentsId)
}
