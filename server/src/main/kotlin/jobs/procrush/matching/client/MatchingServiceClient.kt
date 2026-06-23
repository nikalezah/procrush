package jobs.procrush.matching.client

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import jobs.procrush.matching.dto.CandidateRecommendationDto
import jobs.procrush.matching.dto.JobRecommendationDto
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

class MatchingServiceClient(
    private val baseUrl: String,
    private val httpClient: HttpClient = defaultClient(),
) {
    private val logger = LoggerFactory.getLogger(MatchingServiceClient::class.java)

    suspend fun jobRecommendationsForSeeker(seekerId: Long): List<JobRecommendationDto> =
        runCatching {
            httpClient
                .get("$baseUrl/internal/seekers/$seekerId/recommendations")
                .body<List<JobRecommendationDto>>()
        }.getOrElse { error ->
            logger.error("Failed to fetch seeker recommendations seekerId={}", seekerId, error)
            emptyList()
        }

    suspend fun candidateRecommendationsForJob(jobProfileId: Long): List<CandidateRecommendationDto> =
        runCatching {
            httpClient
                .get("$baseUrl/internal/job-profiles/$jobProfileId/candidates")
                .body<List<CandidateRecommendationDto>>()
        }.getOrElse { error ->
            logger.error("Failed to fetch job candidates jobProfileId={}", jobProfileId, error)
            emptyList()
        }

    suspend fun jobRecommendationForSeeker(seekerId: Long, jobProfileId: Long): JobRecommendationDto? =
        fetchOrNull("$baseUrl/internal/seekers/$seekerId/jobs/$jobProfileId")

    suspend fun candidateRecommendationForJob(seekerId: Long, jobProfileId: Long): CandidateRecommendationDto? =
        fetchOrNull("$baseUrl/internal/seekers/$seekerId/jobs/$jobProfileId/candidate")

    suspend fun countMatchedCandidatesForOccupation(occupationId: Long): Int =
        runCatching {
            httpClient
                .get("$baseUrl/internal/occupations/$occupationId/candidate-count")
                .body<Map<String, Int>>()["count"] ?: 0
        }.getOrElse { 0 }

    suspend fun countMatchedCandidatesForOccupations(occupationIds: List<Long>): Map<Long, Int> {
        if (occupationIds.isEmpty()) return emptyMap()
        return runCatching {
            httpClient
                .get("$baseUrl/internal/occupations/candidate-counts?ids=${occupationIds.joinToString(",")}")
                .body<Map<Long, Int>>()
        }.getOrElse { occupationIds.associateWith { 0 } }
    }

    fun close() {
        httpClient.close()
    }

    private suspend inline fun <reified T> fetchOrNull(url: String): T? {
        val response: HttpResponse = httpClient.get(url)
        if (response.status == HttpStatusCode.NotFound) return null
        return response.body()
    }

    companion object {
        private fun defaultClient(): HttpClient =
            HttpClient(CIO) {
                install(ContentNegotiation) {
                    json(
                        Json {
                            ignoreUnknownKeys = true
                        },
                    )
                }
            }
    }
}
