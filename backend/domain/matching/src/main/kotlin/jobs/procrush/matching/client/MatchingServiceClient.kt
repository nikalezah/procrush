package jobs.procrush.matching.client

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
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
        fetchList("$baseUrl/internal/seekers/$seekerId/recommendations")

    suspend fun candidateRecommendationsForJob(jobProfileId: Long): List<CandidateRecommendationDto> =
        fetchList("$baseUrl/internal/job-profiles/$jobProfileId/candidates")

    suspend fun jobRecommendationForSeeker(seekerId: Long, jobProfileId: Long): JobRecommendationDto? =
        fetchOrNull("$baseUrl/internal/seekers/$seekerId/jobs/$jobProfileId")

    suspend fun candidateRecommendationForJob(seekerId: Long, jobProfileId: Long): CandidateRecommendationDto? =
        fetchOrNull("$baseUrl/internal/seekers/$seekerId/jobs/$jobProfileId/candidate")

    suspend fun jobRecommendationDisplay(jobProfileId: Long): JobRecommendationDto? =
        fetchOrNull("$baseUrl/internal/job-profiles/$jobProfileId/display")

    suspend fun candidateRecommendationDisplay(seekerId: Long, jobProfileId: Long): CandidateRecommendationDto? =
        fetchOrNull("$baseUrl/internal/seekers/$seekerId/jobs/$jobProfileId/display-candidate")

    suspend fun countMatchedCandidatesForOccupation(occupationId: Long): Int {
        val response: HttpResponse = httpClient.get("$baseUrl/internal/occupations/$occupationId/candidate-count")
        if (!response.status.isSuccess()) {
            error("Matching service request failed: ${response.status}")
        }
        return response.body<Map<String, Int>>()["count"] ?: 0
    }

    suspend fun countMatchedCandidatesForOccupations(occupationIds: List<Long>): Map<Long, Int> {
        if (occupationIds.isEmpty()) return emptyMap()
        val response: HttpResponse =
            httpClient.get("$baseUrl/internal/occupations/candidate-counts?ids=${occupationIds.joinToString(",")}")
        if (!response.status.isSuccess()) {
            error("Matching service request failed: ${response.status}")
        }
        return response.body()
    }

    suspend fun pingHealth(): Boolean {
        val response = httpClient.get("$baseUrl/health")
        return response.status == HttpStatusCode.OK
    }

    fun close() {
        httpClient.close()
    }

    private suspend inline fun <reified T> fetchOrNull(url: String): T? {
        val response: HttpResponse = httpClient.get(url)
        if (response.status == HttpStatusCode.NotFound) return null
        if (!response.status.isSuccess()) {
            error("Matching service request failed: ${response.status} $url")
        }
        return response.body()
    }

    private suspend inline fun <reified T> fetchList(url: String): List<T> {
        val response: HttpResponse = httpClient.get(url)
        if (!response.status.isSuccess()) {
            logger.error("Matching service list request failed: {} {}", response.status, url)
            error("Matching service request failed: ${response.status} $url")
        }
        return response.body()
    }

    private suspend fun fetchMap(url: String): Map<String, Int> {
        val response: HttpResponse = httpClient.get(url)
        if (!response.status.isSuccess()) {
            error("Matching service request failed: ${response.status} $url")
        }
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
