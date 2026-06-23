package jobs.procrush.matching.runtime

import jobs.procrush.bootstrap.config.DatabaseConfig
import jobs.procrush.matching.events.JobProfileChangedPayload
import jobs.procrush.matching.events.SeekerProfileChangedPayload
import jobs.procrush.matching.runtime.bootstrap.MatchingDatabaseRegistry
import jobs.procrush.matching.runtime.repository.MatchResultsRepository
import jobs.procrush.matching.runtime.repository.MatchingProjectionRepository
import jobs.procrush.matching.runtime.service.MatchingEventProcessor
import jobs.procrush.personality.dto.PersonalityAxesDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MatchingEventProcessorTest {
    @Container
    val matchingPostgres =
        PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"))
            .withDatabaseName("procrush_matching")
            .withUsername("procrush")
            .withPassword("procrush")

    private lateinit var repository: MatchResultsRepository
    private lateinit var processor: MatchingEventProcessor

    @BeforeAll
    fun setup() {
        matchingPostgres.start()
        val matchingConfig =
            DatabaseConfig(
                jdbcUrl = matchingPostgres.jdbcUrl,
                user = matchingPostgres.username,
                password = matchingPostgres.password,
            )
        MatchingDatabaseRegistry.init(matchingConfig)
        repository = MatchResultsRepository()
        processor =
            MatchingEventProcessor(
                projectionRepository = MatchingProjectionRepository(),
                matchResultsRepository = repository,
            )
    }

    @Test
    fun `seeker and job events produce stored match row`() {
        val jobPayload =
            JobProfileChangedPayload(
                jobProfileId = 88_001,
                occupationId = 1,
                skillIds = listOf(1L),
                personalityAxes = PersonalityAxesDto.DEFAULT,
                isActive = true,
                companyName = "Acme",
                occupationName = "Developer",
                description = "Kotlin backend",
                deleted = false,
            )
        processor.processJobProfileChanged(jobPayload)

        processor.processSeekerProfileChanged(
            SeekerProfileChangedPayload(
                seekerId = 42,
                desiredOccupationIds = listOf(1),
                skillIds = listOf(1L),
                personalityReady = false,
                firstName = "Ann",
                lastName = "Bee",
                skillNames = listOf("Kotlin"),
            ),
        )

        val stored = repository.findPair(42, 88_001)
        assertTrue(stored != null)
        assertEquals("Acme", stored!!.companyName)
        assertEquals("Ann", stored.seekerFirstName)
        assertTrue(stored.matchScoreDisplay in 1..100)
    }

    @Test
    fun `job event matches only eligible seekers`() {
        processor.processSeekerProfileChanged(
            SeekerProfileChangedPayload(
                seekerId = 43,
                desiredOccupationIds = listOf(2),
                skillIds = listOf(1L),
                personalityReady = false,
                firstName = "Bob",
                lastName = "Cee",
                skillNames = listOf("Kotlin"),
                matchingEligible = false,
            ),
        )

        val jobPayload =
            JobProfileChangedPayload(
                jobProfileId = 88_002,
                occupationId = 2,
                skillIds = listOf(1L),
                personalityAxes = PersonalityAxesDto.DEFAULT,
                isActive = true,
                companyName = "Beta",
                occupationName = "Analyst",
                deleted = false,
            )
        processor.processJobProfileChanged(jobPayload)
        assertTrue(repository.findPair(43, 88_002) == null)

        processor.processSeekerProfileChanged(
            SeekerProfileChangedPayload(
                seekerId = 43,
                desiredOccupationIds = listOf(2),
                skillIds = listOf(1L),
                personalityReady = false,
                firstName = "Bob",
                lastName = "Cee",
                skillNames = listOf("Kotlin"),
                matchingEligible = true,
            ),
        )
        assertTrue(repository.findPair(43, 88_002) != null)
    }
}
