package jobs.procrush.personality.llm

import jobs.procrush.personality.dto.PersonalityProfileStatus
import jobs.procrush.personality.dto.SeekerPersonalProfileLlmOutput
import jobs.procrush.personality.dto.SeekerPersonalProfileRecord

object PersonalityProfileLlmMapper {
    fun fromLlmOutput(seekerId: Long, output: SeekerPersonalProfileLlmOutput): SeekerPersonalProfileRecord =
        SeekerPersonalProfileRecord(
            seekerId = seekerId,
            title = output.title,
            description = output.description,
            profile = output.profile,
            autonomy = output.autonomy,
            thinkingStyle = output.thinkingStyle,
            burnoutRisk = output.burnoutRisk,
            connections = output.connections,
            creativity = output.creativity,
            drive = output.drive,
            thinking = output.thinking,
            axisDominance = output.axisDominance,
            axisInfluence = output.axisInfluence,
            axisStability = output.axisStability,
            axisIntegrity = output.axisIntegrity,
            axisAutonomy = output.axisAutonomy,
            axisPace = output.axisPace,
            burnoutRiskOverload = output.burnoutRiskOverload,
            burnoutRiskConflicts = output.burnoutRiskConflicts,
            burnoutRiskDemotivation = output.burnoutRiskDemotivation,
            burnoutRiskStress = output.burnoutRiskStress,
            energySources = output.energySources,
            stopFactors = output.stopFactors,
            generationStatus = PersonalityProfileStatus.READY,
            generationError = null,
        )
}
