package jobs.procrush.matching.service

import jobs.procrush.personality.dto.PersonalityAxesDto
import kotlin.math.abs

object MatchScoringService {
    fun skillsScore(
        seekerSkillIds: Set<Long>,
        jobSkillIds: Set<Long>,
    ): Double {
        if (jobSkillIds.isEmpty()) return 1.0
        if (seekerSkillIds.isEmpty()) return 0.0
        val intersection = seekerSkillIds.intersect(jobSkillIds).size
        val union = seekerSkillIds.union(jobSkillIds).size
        if (union == 0) return 0.0
        return intersection.toDouble() / union
    }

    fun personalityScore(
        seekerAxes: PersonalityAxesDto,
        employerAxes: PersonalityAxesDto,
    ): Double {
        val diffs =
            seekerAxes.asList().zip(employerAxes.asList()) { seeker, employer ->
                abs(seeker - employer)
            }
        return 1.0 - diffs.average()
    }

    fun combinedScore(
        skills: Double,
        personality: Double?,
        personalityReady: Boolean,
    ): Double =
        if (personalityReady && personality != null) {
            0.5 * skills + 0.5 * personality
        } else {
            skills
        }
}
