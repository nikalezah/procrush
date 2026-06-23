package jobs.procrush.seeker.tables

import jobs.procrush.personality.dto.ConnectionsCategory
import jobs.procrush.personality.dto.CreativityCategory
import jobs.procrush.personality.dto.DriveCategory
import jobs.procrush.personality.dto.EnergySourcesSection
import jobs.procrush.personality.dto.PersonalityDbJson
import jobs.procrush.personality.dto.StopFactorsSection
import jobs.procrush.personality.dto.ThinkingCategory
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone
import org.jetbrains.exposed.v1.json.jsonb

object SeekerPersonalProfilesTable : Table("seeker_personal_profiles") {
    val seekerId = reference("seeker_id", SeekersTable, onDelete = ReferenceOption.CASCADE)
    val title = varchar("title", 255).nullable()
    val description = text("description").nullable()
    val profile = text("profile").nullable()
    val autonomy = text("autonomy").nullable()
    val thinkingStyle = text("thinking_style").nullable()
    val burnoutRisk = text("burnout_risk").nullable()
    val connections = jsonb<ConnectionsCategory>("connections", PersonalityDbJson).nullable()
    val creativity = jsonb<CreativityCategory>("creativity", PersonalityDbJson).nullable()
    val drive = jsonb<DriveCategory>("drive", PersonalityDbJson).nullable()
    val thinking = jsonb<ThinkingCategory>("thinking", PersonalityDbJson).nullable()
    val axisDominance = decimal("axis_dominance", 3, 2).nullable()
    val axisInfluence = decimal("axis_influence", 3, 2).nullable()
    val axisStability = decimal("axis_stability", 3, 2).nullable()
    val axisIntegrity = decimal("axis_integrity", 3, 2).nullable()
    val axisAutonomy = decimal("axis_autonomy", 3, 2).nullable()
    val axisPace = decimal("axis_pace", 3, 2).nullable()
    val burnoutRiskOverload = decimal("burnout_risk_overload", 3, 2).nullable()
    val burnoutRiskConflicts = decimal("burnout_risk_conflicts", 3, 2).nullable()
    val burnoutRiskDemotivation = decimal("burnout_risk_demotivation", 3, 2).nullable()
    val burnoutRiskStress = decimal("burnout_risk_stress", 3, 2).nullable()
    val energySources = jsonb<EnergySourcesSection>("energy_sources", PersonalityDbJson).nullable()
    val stopFactors = jsonb<StopFactorsSection>("stop_factors", PersonalityDbJson).nullable()
    val generationStatus = varchar("generation_status", 20)
    val generationError = text("generation_error").nullable()
    val updatedAt = timestampWithTimeZone("updated_at")

    override val primaryKey = PrimaryKey(seekerId)
}
