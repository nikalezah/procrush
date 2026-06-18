package jobs.procrush.matching.service

import jobs.procrush.personality.dto.PersonalityAxesDto
import kotlin.test.Test
import kotlin.test.assertEquals

class MatchScoringServiceTest {
    @Test
    fun skillsScoreFullOverlap() {
        val score = MatchScoringService.skillsScore(setOf(1L, 2L, 3L), setOf(1L, 2L, 3L))
        assertEquals(1.0, score)
    }

    @Test
    fun skillsScorePartialOverlap() {
        val score = MatchScoringService.skillsScore(setOf(1L, 2L), setOf(2L, 3L))
        assertEquals(1.0 / 3.0, score)
    }

    @Test
    fun skillsScoreEmptyJobSkillsReturnsOne() {
        assertEquals(1.0, MatchScoringService.skillsScore(setOf(1L, 2L), emptySet()))
    }

    @Test
    fun skillsScoreEmptySeekerSkillsReturnsZero() {
        assertEquals(0.0, MatchScoringService.skillsScore(emptySet(), setOf(1L, 2L)))
    }

    @Test
    fun personalityScoreIdenticalAxesReturnsOne() {
        val axes = PersonalityAxesDto.DEFAULT
        assertEquals(1.0, MatchScoringService.personalityScore(axes, axes))
    }

    @Test
    fun personalityScoreOppositeAxesReturnsZero() {
        val seeker =
            PersonalityAxesDto(
                axisDominance = 0.0,
                axisInfluence = 0.0,
                axisStability = 0.0,
                axisIntegrity = 0.0,
                axisAutonomy = 0.0,
                axisPace = 0.0,
            )
        val employer =
            PersonalityAxesDto(
                axisDominance = 1.0,
                axisInfluence = 1.0,
                axisStability = 1.0,
                axisIntegrity = 1.0,
                axisAutonomy = 1.0,
                axisPace = 1.0,
            )
        assertEquals(0.0, MatchScoringService.personalityScore(seeker, employer))
    }

    @Test
    fun combinedScoreUsesPersonalityWhenReady() {
        val combined = MatchScoringService.combinedScore(0.8, 0.6, personalityReady = true)
        assertEquals(0.7, combined)
    }

    @Test
    fun combinedScoreUsesSkillsOnlyWhenPersonalityNotReady() {
        val combined = MatchScoringService.combinedScore(0.8, 0.6, personalityReady = false)
        assertEquals(0.8, combined)
    }

    @Test
    fun toDisplayScoreMapsRange() {
        assertEquals(1, MatchScoringService.toDisplayScore(0.0))
        assertEquals(100, MatchScoringService.toDisplayScore(1.0))
        assertEquals(83, MatchScoringService.toDisplayScore(0.825))
    }
}
