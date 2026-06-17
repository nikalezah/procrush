package jobs.procrush.db

import jobs.procrush.db.tables.OccupationsTable
import jobs.procrush.db.tables.SkillsTable
import jobs.procrush.db.tables.SuperpowersAndTalentsTable
import jobs.procrush.models.OccupationDto
import jobs.procrush.models.SkillDto
import jobs.procrush.models.SuperpowerAndTalentDto
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class ReferenceRepository {
    fun listOccupations(leafOnly: Boolean): List<OccupationDto> =
        transaction {
            val query =
                if (leafOnly) {
                    OccupationsTable.selectAll().where { OccupationsTable.isLeaf eq true }
                } else {
                    OccupationsTable.selectAll()
                }
            query
                .orderBy(OccupationsTable.name to SortOrder.ASC)
                .map { it.toOccupationDto() }
        }

    fun findOccupationsByIds(ids: List<Long>): List<OccupationDto> =
        transaction {
            if (ids.isEmpty()) return@transaction emptyList()
            OccupationsTable
                .selectAll()
                .where { OccupationsTable.id inList ids }
                .map { it.toOccupationDto() }
        }

    fun findOccupationById(id: Long): OccupationDto? =
        transaction {
            OccupationsTable
                .selectAll()
                .where { OccupationsTable.id eq id }
                .firstOrNull()
                ?.toOccupationDto()
        }

    fun searchSkills(query: String?, limit: Int = 50): List<SkillDto> =
        transaction {
            val base =
                if (query.isNullOrBlank()) {
                    SkillsTable.selectAll()
                } else {
                    SkillsTable.selectAll().where {
                        SkillsTable.name.lowerCase() like "%${query.lowercase()}%"
                    }
                }
            base
                .orderBy(SkillsTable.name to SortOrder.ASC)
                .limit(limit)
                .map { it.toSkillDto() }
        }

    fun findSkillsByIds(ids: List<Long>): List<SkillDto> =
        transaction {
            if (ids.isEmpty()) return@transaction emptyList()
            SkillsTable
                .selectAll()
                .where { SkillsTable.id inList ids }
                .map { it.toSkillDto() }
        }

    fun listSuperpowersAndTalents(): List<SuperpowerAndTalentDto> =
        transaction {
            SuperpowersAndTalentsTable
                .selectAll()
                .orderBy(SuperpowersAndTalentsTable.name to SortOrder.ASC)
                .map { it.toSuperpowerAndTalentDto() }
        }

    fun findSuperpowersAndTalentsByNames(names: List<String>): Map<String, Long> =
        transaction {
            if (names.isEmpty()) return@transaction emptyMap()
            SuperpowersAndTalentsTable
                .selectAll()
                .where { SuperpowersAndTalentsTable.name inList names }
                .associate { row ->
                    row[SuperpowersAndTalentsTable.name]!! to row[SuperpowersAndTalentsTable.id].value
                }
        }

    private fun ResultRow.toOccupationDto() =
        OccupationDto(
            id = this[OccupationsTable.id].value,
            parentId = this[OccupationsTable.parentId],
            name = this[OccupationsTable.name],
            isLeaf = this[OccupationsTable.isLeaf],
        )

    private fun ResultRow.toSkillDto() =
        SkillDto(
            id = this[SkillsTable.id].value,
            name = this[SkillsTable.name],
        )

    private fun ResultRow.toSuperpowerAndTalentDto() =
        SuperpowerAndTalentDto(
            id = this[SuperpowersAndTalentsTable.id].value,
            name = this[SuperpowersAndTalentsTable.name]!!,
        )
}
