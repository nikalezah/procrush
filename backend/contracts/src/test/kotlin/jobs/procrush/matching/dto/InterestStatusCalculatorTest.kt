package jobs.procrush.matching.dto

import kotlin.test.Test
import kotlin.test.assertEquals

class InterestStatusCalculatorTest {
    @Test
    fun forSeekerNoneWhenNoResponses() {
        assertEquals(InterestStatus.NONE, InterestStatusCalculator.forSeeker(false, false))
    }

    @Test
    fun forSeekerRespondedWhenOnlySeekerResponded() {
        assertEquals(InterestStatus.RESPONDED, InterestStatusCalculator.forSeeker(true, false))
    }

    @Test
    fun forSeekerIncomingWhenOnlyEmployerResponded() {
        assertEquals(InterestStatus.INCOMING, InterestStatusCalculator.forSeeker(false, true))
    }

    @Test
    fun forSeekerMutualWhenBothResponded() {
        assertEquals(InterestStatus.MUTUAL, InterestStatusCalculator.forSeeker(true, true))
    }

    @Test
    fun forEmployerRespondedWhenOnlyEmployerResponded() {
        assertEquals(InterestStatus.RESPONDED, InterestStatusCalculator.forEmployer(false, true))
    }

    @Test
    fun forEmployerIncomingWhenOnlySeekerResponded() {
        assertEquals(InterestStatus.INCOMING, InterestStatusCalculator.forEmployer(true, false))
    }

    @Test
    fun forEmployerMutualWhenBothResponded() {
        assertEquals(InterestStatus.MUTUAL, InterestStatusCalculator.forEmployer(true, true))
    }
}
