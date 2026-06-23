package jobs.procrush.seeker.tables

import jobs.procrush.shared.tables.OccupationsTable
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object SeekerDesiredPositionsTable : LongIdTable("seeker_desired_positions") {
    val seekerId = reference("seeker_id", SeekersTable, onDelete = ReferenceOption.CASCADE)
    val occupationId = reference("occupation_id", OccupationsTable, onDelete = ReferenceOption.CASCADE)
    val createdAt = timestampWithTimeZone("created_at")

    init {
        uniqueIndex(seekerId, occupationId)
    }
}
