package jobs.procrush.matching.repository

import jobs.procrush.shared.repository.ReferenceRepository
import kotlin.test.Test
import kotlin.test.assertEquals

class MatchingRepositoryTest {
    private val repository = MatchingRepository(ReferenceRepository())

    @Test
    fun countMatchableSeekersByOccupationsReturnsEmptyForEmptyInput() {
        assertEquals(emptyMap(), repository.countMatchableSeekersByOccupations(emptyList()))
    }
}
