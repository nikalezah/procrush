package jobs.procrush.db

import jobs.procrush.db.tables.SeekerPersonalProfilesTable
import jobs.procrush.models.PersonalityProfileStatus
import jobs.procrush.models.SeekerPersonalProfileRecord
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.time.OffsetDateTime

class SeekerPersonalProfileRepository {
    fun findBySeekerId(seekerId: Long): SeekerPersonalProfileRecord? =
        transaction {
            SeekerPersonalProfilesTable
                .selectAll()
                .where { SeekerPersonalProfilesTable.seekerId eq seekerId }
                .firstOrNull()
                ?.toRecord()
        }

    fun insertProcessing(seekerId: Long) {
        transaction {
            val now = OffsetDateTime.now()
            SeekerPersonalProfilesTable.insert {
                it[SeekerPersonalProfilesTable.seekerId] = seekerId
                it[generationStatus] = PersonalityProfileStatus.PROCESSING.name
                it[generationError] = null
                it[updatedAt] = now
            }
        }
    }

    fun markProcessing(seekerId: Long) {
        transaction {
            val now = OffsetDateTime.now()
            val updated =
                SeekerPersonalProfilesTable.update({ SeekerPersonalProfilesTable.seekerId eq seekerId }) {
                    it[generationStatus] = PersonalityProfileStatus.PROCESSING.name
                    it[generationError] = null
                    it[updatedAt] = now
                }
            if (updated == 0) {
                SeekerPersonalProfilesTable.insert {
                    it[SeekerPersonalProfilesTable.seekerId] = seekerId
                    it[generationStatus] = PersonalityProfileStatus.PROCESSING.name
                    it[generationError] = null
                    it[updatedAt] = now
                }
            }
        }
    }

    fun markFailed(seekerId: Long, error: String) {
        transaction {
            val now = OffsetDateTime.now()
            val updated =
                SeekerPersonalProfilesTable.update({ SeekerPersonalProfilesTable.seekerId eq seekerId }) {
                    it[generationStatus] = PersonalityProfileStatus.FAILED.name
                    it[generationError] = error.take(2000)
                    it[updatedAt] = now
                }
            if (updated == 0) {
                SeekerPersonalProfilesTable.insert {
                    it[SeekerPersonalProfilesTable.seekerId] = seekerId
                    it[generationStatus] = PersonalityProfileStatus.FAILED.name
                    it[generationError] = error.take(2000)
                    it[updatedAt] = now
                }
            }
        }
    }

    fun upsertProfile(seekerId: Long, profile: SeekerPersonalProfileRecord) {
        transaction {
            val now = OffsetDateTime.now()
            val updated =
                SeekerPersonalProfilesTable.update({ SeekerPersonalProfilesTable.seekerId eq seekerId }) {
                    it[title] = profile.title
                    it[description] = profile.description
                    it[SeekerPersonalProfilesTable.profile] = profile.profile
                    it[autonomy] = profile.autonomy
                    it[thinkingStyle] = profile.thinkingStyle
                    it[burnoutRisk] = profile.burnoutRisk
                    it[connections] = profile.connections
                    it[creativity] = profile.creativity
                    it[drive] = profile.drive
                    it[thinking] = profile.thinking
                    it[axisDominance] = profile.axisDominance?.toBigDecimal()
                    it[axisInfluence] = profile.axisInfluence?.toBigDecimal()
                    it[axisStability] = profile.axisStability?.toBigDecimal()
                    it[axisIntegrity] = profile.axisIntegrity?.toBigDecimal()
                    it[axisAutonomy] = profile.axisAutonomy?.toBigDecimal()
                    it[axisPace] = profile.axisPace?.toBigDecimal()
                    it[burnoutRiskOverload] = profile.burnoutRiskOverload?.toBigDecimal()
                    it[burnoutRiskConflicts] = profile.burnoutRiskConflicts?.toBigDecimal()
                    it[burnoutRiskDemotivation] = profile.burnoutRiskDemotivation?.toBigDecimal()
                    it[burnoutRiskStress] = profile.burnoutRiskStress?.toBigDecimal()
                    it[energySources] = profile.energySources
                    it[stopFactors] = profile.stopFactors
                    it[generationStatus] = PersonalityProfileStatus.READY.name
                    it[generationError] = null
                    it[updatedAt] = now
                }
            if (updated == 0) {
                SeekerPersonalProfilesTable.insert {
                    it[SeekerPersonalProfilesTable.seekerId] = seekerId
                    it[title] = profile.title
                    it[description] = profile.description
                    it[SeekerPersonalProfilesTable.profile] = profile.profile
                    it[autonomy] = profile.autonomy
                    it[thinkingStyle] = profile.thinkingStyle
                    it[burnoutRisk] = profile.burnoutRisk
                    it[connections] = profile.connections
                    it[creativity] = profile.creativity
                    it[drive] = profile.drive
                    it[thinking] = profile.thinking
                    it[axisDominance] = profile.axisDominance?.toBigDecimal()
                    it[axisInfluence] = profile.axisInfluence?.toBigDecimal()
                    it[axisStability] = profile.axisStability?.toBigDecimal()
                    it[axisIntegrity] = profile.axisIntegrity?.toBigDecimal()
                    it[axisAutonomy] = profile.axisAutonomy?.toBigDecimal()
                    it[axisPace] = profile.axisPace?.toBigDecimal()
                    it[burnoutRiskOverload] = profile.burnoutRiskOverload?.toBigDecimal()
                    it[burnoutRiskConflicts] = profile.burnoutRiskConflicts?.toBigDecimal()
                    it[burnoutRiskDemotivation] = profile.burnoutRiskDemotivation?.toBigDecimal()
                    it[burnoutRiskStress] = profile.burnoutRiskStress?.toBigDecimal()
                    it[energySources] = profile.energySources
                    it[stopFactors] = profile.stopFactors
                    it[generationStatus] = PersonalityProfileStatus.READY.name
                    it[generationError] = null
                    it[updatedAt] = now
                }
            }
        }
    }

    private fun ResultRow.toRecord(): SeekerPersonalProfileRecord =
        SeekerPersonalProfileRecord(
            seekerId = this[SeekerPersonalProfilesTable.seekerId].value,
            title = this[SeekerPersonalProfilesTable.title],
            description = this[SeekerPersonalProfilesTable.description],
            profile = this[SeekerPersonalProfilesTable.profile],
            autonomy = this[SeekerPersonalProfilesTable.autonomy],
            thinkingStyle = this[SeekerPersonalProfilesTable.thinkingStyle],
            burnoutRisk = this[SeekerPersonalProfilesTable.burnoutRisk],
            connections = this[SeekerPersonalProfilesTable.connections],
            creativity = this[SeekerPersonalProfilesTable.creativity],
            drive = this[SeekerPersonalProfilesTable.drive],
            thinking = this[SeekerPersonalProfilesTable.thinking],
            axisDominance = this[SeekerPersonalProfilesTable.axisDominance]?.toDouble(),
            axisInfluence = this[SeekerPersonalProfilesTable.axisInfluence]?.toDouble(),
            axisStability = this[SeekerPersonalProfilesTable.axisStability]?.toDouble(),
            axisIntegrity = this[SeekerPersonalProfilesTable.axisIntegrity]?.toDouble(),
            axisAutonomy = this[SeekerPersonalProfilesTable.axisAutonomy]?.toDouble(),
            axisPace = this[SeekerPersonalProfilesTable.axisPace]?.toDouble(),
            burnoutRiskOverload = this[SeekerPersonalProfilesTable.burnoutRiskOverload]?.toDouble(),
            burnoutRiskConflicts = this[SeekerPersonalProfilesTable.burnoutRiskConflicts]?.toDouble(),
            burnoutRiskDemotivation = this[SeekerPersonalProfilesTable.burnoutRiskDemotivation]?.toDouble(),
            burnoutRiskStress = this[SeekerPersonalProfilesTable.burnoutRiskStress]?.toDouble(),
            energySources = this[SeekerPersonalProfilesTable.energySources],
            stopFactors = this[SeekerPersonalProfilesTable.stopFactors],
            generationStatus =
                PersonalityProfileStatus.valueOf(this[SeekerPersonalProfilesTable.generationStatus]),
            generationError = this[SeekerPersonalProfilesTable.generationError],
        )
}
