package jobs.procrush.survey.route

import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import jobs.procrush.auth.UserRole
import jobs.procrush.auth.service.RoleGuard
import jobs.procrush.bootstrap.route.requireLongParam
import jobs.procrush.personality.service.PersonalityProfileService
import jobs.procrush.survey.dto.CompleteSurveyResponseDto
import jobs.procrush.survey.dto.SaveSurveyAnswersRequest
import jobs.procrush.survey.dto.SurveyStatus
import jobs.procrush.survey.service.SurveyService

fun Route.seekerSurveyRoutes(
    roleGuard: RoleGuard,
    surveyService: SurveyService,
    personalityProfileService: PersonalityProfileService,
) {
    route("/api/seeker") {
        get("/personality/llm-context") {
            val user = roleGuard.requireRole(call, UserRole.SEEKER) ?: return@get
            call.respond(personalityProfileService.buildLlmContext(user.id))
        }
        get("/surveys") {
            val user = roleGuard.requireRole(call, UserRole.SEEKER) ?: return@get
            call.respond(surveyService.listGroups(user.id))
        }
        get("/surveys/{id}") {
            val user = roleGuard.requireRole(call, UserRole.SEEKER) ?: return@get
            val id = call.requireLongParam() ?: return@get
            call.respond(surveyService.getSurvey(user.id, id))
        }
        post("/surveys/{id}/start") {
            val user = roleGuard.requireRole(call, UserRole.SEEKER) ?: return@post
            val id = call.requireLongParam() ?: return@post
            call.respond(surveyService.startSurvey(user.id, id))
        }
        put("/surveys/{id}/answers") {
            val user = roleGuard.requireRole(call, UserRole.SEEKER) ?: return@put
            val id = call.requireLongParam() ?: return@put
            val body = call.receive<SaveSurveyAnswersRequest>()
            call.respond(surveyService.saveAnswers(user.id, id, body))
        }
        post("/surveys/{id}/complete") {
            val user = roleGuard.requireRole(call, UserRole.SEEKER) ?: return@post
            val id = call.requireLongParam() ?: return@post
            val body = call.receive<SaveSurveyAnswersRequest>()
            val result = surveyService.completeSurvey(user.id, id, body)
            call.respond(
                CompleteSurveyResponseDto(
                    resultId = result.result.id,
                    surveyId = result.result.surveyId,
                    status = SurveyStatus.COMPLETED,
                    nextSurveyId = result.nextSurveyId,
                ),
            )
        }
    }
}
