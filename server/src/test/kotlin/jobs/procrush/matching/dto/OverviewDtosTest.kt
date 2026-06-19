package jobs.procrush.matching.dto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OverviewDtosTest {
    @Test
    fun employerCandidatesOverviewDtoHoldsCandidatesAndInterests() {
        val candidate =
            CandidateRecommendationDto(
                id = 1,
                firstName = "Anna",
                lastName = "Ivanova",
                positionName = "Developer",
                skills = listOf("Kotlin"),
                matchScore = 0.8,
                matchScoreDisplay = 80,
            )
        val overview =
            EmployerCandidatesOverviewDto(
                candidates = listOf(candidate),
                interests =
                    EmployerInterestsResponseDto(
                        respondedOutside = emptyList(),
                        mutualOutside = emptyList(),
                    ),
            )

        assertEquals(1, overview.candidates.size)
        assertTrue(overview.interests.respondedOutside.isEmpty())
        assertTrue(overview.interests.mutualOutside.isEmpty())
    }
}
