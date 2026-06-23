package jobs.procrush.matching.runtime.route

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import jobs.procrush.matching.dto.CandidateRecommendationDto
import jobs.procrush.matching.dto.JobRecommendationDto
import jobs.procrush.matching.runtime.repository.MatchResultsRepository
import kotlinx.serialization.json.Json

fun Route.matchingReadRoutes(repository: MatchResultsRepository) {
    val json = Json { ignoreUnknownKeys = true }

    route("/internal") {
        get("/seekers/{seekerId}/recommendations") {
            val seekerId = call.parameters["seekerId"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Некорректный seekerId"))
            val results = repository.listForSeeker(seekerId)
            call.respond(
                results.map { row ->
                    JobRecommendationDto(
                        id = row.jobProfileId,
                        companyName = row.companyName,
                        positionName = row.positionName,
                        description = row.jobDescription,
                        matchScore = row.matchScore,
                        matchScoreDisplay = row.matchScoreDisplay,
                    )
                },
            )
        }
        get("/job-profiles/{jobProfileId}/candidates") {
            val jobProfileId = call.parameters["jobProfileId"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Некорректный jobProfileId"))
            val results = repository.listForJob(jobProfileId)
            call.respond(
                results.map { row ->
                    CandidateRecommendationDto(
                        id = row.seekerId,
                        firstName = row.seekerFirstName,
                        lastName = row.seekerLastName,
                        positionName = row.positionName,
                        skills = json.decodeFromString<List<String>>(row.seekerSkillsJson),
                        matchScore = row.matchScore,
                        matchScoreDisplay = row.matchScoreDisplay,
                    )
                },
            )
        }
        get("/seekers/{seekerId}/jobs/{jobProfileId}") {
            val seekerId = call.parameters["seekerId"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Некорректный seekerId"))
            val jobProfileId = call.parameters["jobProfileId"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Некорректный jobProfileId"))
            val row = repository.findPair(seekerId, jobProfileId)
                ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("message" to "Не найдено"))
            call.respond(
                JobRecommendationDto(
                    id = row.jobProfileId,
                    companyName = row.companyName,
                    positionName = row.positionName,
                    description = row.jobDescription,
                    matchScore = row.matchScore,
                    matchScoreDisplay = row.matchScoreDisplay,
                ),
            )
        }
        get("/seekers/{seekerId}/jobs/{jobProfileId}/candidate") {
            val seekerId = call.parameters["seekerId"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Некорректный seekerId"))
            val jobProfileId = call.parameters["jobProfileId"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Некорректный jobProfileId"))
            val row = repository.findPair(seekerId, jobProfileId)
                ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("message" to "Не найдено"))
            call.respond(
                CandidateRecommendationDto(
                    id = row.seekerId,
                    firstName = row.seekerFirstName,
                    lastName = row.seekerLastName,
                    positionName = row.positionName,
                    skills = json.decodeFromString<List<String>>(row.seekerSkillsJson),
                    matchScore = row.matchScore,
                    matchScoreDisplay = row.matchScoreDisplay,
                ),
            )
        }
        get("/occupations/{occupationId}/candidate-count") {
            val occupationId = call.parameters["occupationId"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Некорректный occupationId"))
            call.respond(mapOf("count" to repository.countDistinctSeekersForOccupation(occupationId)))
        }
        get("/occupations/candidate-counts") {
            val raw = call.request.queryParameters["ids"] ?: ""
            val occupationIds = raw.split(',').mapNotNull { it.trim().toLongOrNull() }
            call.respond(repository.countDistinctSeekersForOccupations(occupationIds))
        }
    }
}
