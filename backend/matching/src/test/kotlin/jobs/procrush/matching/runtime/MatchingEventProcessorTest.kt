package jobs.procrush.matching.runtime

import jobs.procrush.bootstrap.config.DatabaseConfig
import jobs.procrush.matching.events.JobProfileChangedPayload
import jobs.procrush.matching.events.SeekerProfileChangedPayload
import jobs.procrush.matching.repository.MatchingRepository
import jobs.procrush.matching.runtime.bootstrap.MatchingDatabaseRegistry
import jobs.procrush.matching.runtime.repository.MatchResultsRepository
import jobs.procrush.matching.runtime.service.MatchingEventProcessor
import jobs.procrush.personality.dto.PersonalityAxesDto
import jobs.procrush.shared.repository.ReferenceRepository
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
        val mainConfig =
            DatabaseConfig.resolve(
                databaseUrl = System.getenv("DATABASE_URL"),
                databaseUser = System.getenv("DATABASE_USER"),
                databasePassword = System.getenv("DATABASE_PASSWORD"),
            )
        val matchingConfig =
            DatabaseConfig(
                jdbcUrl = matchingPostgres.jdbcUrl,
                user = matchingPostgres.username,
                password = matchingPostgres.password,
            )
        MatchingDatabaseRegistry.init(mainConfig, matchingConfig)
        repository = MatchResultsRepository()
        val mainDb = MatchingDatabaseRegistry.main
        val referenceRepository = ReferenceRepository(mainDb)
        processor =
            MatchingEventProcessor(
                matchingRepository = MatchingRepository(referenceRepository, mainDb),
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
}
