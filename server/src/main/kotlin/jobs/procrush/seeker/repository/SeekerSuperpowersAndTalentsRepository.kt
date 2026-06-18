package jobs.procrush.seeker.repository

import jobs.procrush.seeker.tables.SeekerSuperpowersAndTalentsTable
import jobs.procrush.shared.dto.SuperpowerAndTalentDto
import jobs.procrush.shared.tables.SuperpowersAndTalentsTable
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.OffsetDateTime

class SeekerSuperpowersAndTalentsRepository {
    fun findBySeekerId(seekerId: Long): List<SuperpowerAndTalentDto> =
        transaction {
            SeekerSuperpowersAndTalentsTable
                .innerJoin(SuperpowersAndTalentsTable)
                .selectAll()
                .where { SeekerSuperpowersAndTalentsTable.seekerPersonalProfileId eq seekerId }
                .orderBy(
                    SeekerSuperpowersAndTalentsTable.isPronounced to SortOrder.DESC_NULLS_LAST,
                    SuperpowersAndTalentsTable.name to SortOrder.ASC,
                )
                .map { it.toSuperpowerAndTalentDto() }
        }

    fun replaceForSeeker(seekerId: Long, items: List<Pair<Long, Boolean>>) {
        SeekerSuperpowersAndTalentsTable.deleteWhere {
            SeekerSuperpowersAndTalentsTable.seekerPersonalProfileId eq seekerId
        }
        val now = OffsetDateTime.now()
        items.distinctBy { it.first }.forEach { (superpowerId, isPronounced) ->
            SeekerSuperpowersAndTalentsTable.insert {
                it[seekerPersonalProfileId] = seekerId
                it[superpowersAndTalentsId] = superpowerId
                it[SeekerSuperpowersAndTalentsTable.isPronounced] = isPronounced
                it[createdAt] = now
                it[updatedAt] = now
            }
        }
    }

    private fun ResultRow.toSuperpowerAndTalentDto() =
        SuperpowerAndTalentDto(
            id = this[SuperpowersAndTalentsTable.id].value,
            name = this[SuperpowersAndTalentsTable.name]!!,
            isPronounced = this[SeekerSuperpowersAndTalentsTable.isPronounced],
        )
}
