package jobs.procrush.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import jobs.procrush.auth.RoleGuard
import jobs.procrush.auth.UserRole
import jobs.procrush.db.ReferenceRepository
import jobs.procrush.domain.PersonalityProfileService
import jobs.procrush.domain.SeekerProfileService
import jobs.procrush.domain.SurveyService
import jobs.procrush.models.CreateSeekerEducationRequest
import jobs.procrush.models.CreateSeekerExperienceRequest
import jobs.procrush.models.UpdateSeekerDesiredPositionsRequest
import jobs.procrush.models.UpdateSeekerEducationRequest
import jobs.procrush.models.UpdateSeekerExperienceRequest
import jobs.procrush.models.UpdateSeekerProfileRequest
import jobs.procrush.models.UpdateSeekerSkillsRequest
import jobs.procrush.survey.SaveSurveyAnswersRequest

fun Route.referenceRoutes(
    roleGuard: RoleGuard,
    referenceRepository: ReferenceRepository,
) {
    route("/api") {
        get("/occupations") {
            roleGuard.requireAuth(call) ?: return@get
            val leafOnly = call.request.queryParameters["leafOnly"]?.toBooleanStrictOrNull() ?: false
            call.respond(referenceRepository.listOccupations(leafOnly))
        }
        get("/skills") {
            roleGuard.requireAuth(call) ?: return@get
            val query = call.request.queryParameters["q"]
            call.respond(referenceRepository.searchSkills(query))
        }
    }
}

fun Route.seekerRoutes(
    roleGuard: RoleGuard,
    seekerProfileService: SeekerProfileService,
    surveyService: SurveyService,
    personalityProfileService: PersonalityProfileService,
) {
    route("/api/seeker") {
        get("/dashboard") {
            val user = roleGuard.requireRole(call, UserRole.SEEKER) ?: return@get
            call.respond(seekerProfileService.dashboard(user.id))
        }
        get("/me") {
            val user = roleGuard.requireRole(call, UserRole.SEEKER) ?: return@get
            call.respond(seekerProfileService.getOrCreateSeeker(user.id))
        }
        patch("/me") {
            val user = roleGuard.requireRole(call, UserRole.SEEKER) ?: return@patch
            val body = call.receive<UpdateSeekerProfileRequest>()
            try {
                seekerProfileService.updateProfile(user.id, body)
            } catch (e: IllegalArgumentException) {
                return@patch call.respond(HttpStatusCode.BadRequest, mapOf("message" to (e.message ?: "Некорректные данные")))
            }
            call.respond(seekerProfileService.getOrCreateSeeker(user.id))
        }
        get("/experience") {
            val user = roleGuard.requireRole(call, UserRole.SEEKER) ?: return@get
            call.respond(seekerProfileService.listExperience(user.id))
        }
        post("/experience") {
            val user = roleGuard.requireRole(call, UserRole.SEEKER) ?: return@post
            val body = call.receive<CreateSeekerExperienceRequest>()
            call.respond(HttpStatusCode.Created, seekerProfileService.createExperience(user.id, body))
        }
        patch("/experience/{id}") {
            val user = roleGuard.requireRole(call, UserRole.SEEKER) ?: return@patch
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@patch call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Некорректный id"))
            val body = call.receive<UpdateSeekerExperienceRequest>()
            try {
                call.respond(seekerProfileService.updateExperience(user.id, id, body))
            } catch (e: IllegalStateException) {
                call.respond(HttpStatusCode.NotFound, mapOf("message" to (e.message ?: "Не найдено")))
            }
        }
        delete("/experience/{id}") {
            val user = roleGuard.requireRole(call, UserRole.SEEKER) ?: return@delete
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Некорректный id"))
            try {
                seekerProfileService.deleteExperience(user.id, id)
                call.respond(HttpStatusCode.NoContent)
            } catch (e: IllegalStateException) {
                call.respond(HttpStatusCode.NotFound, mapOf("message" to (e.message ?: "Не найдено")))
            }
        }
        get("/education") {
            val user = roleGuard.requireRole(call, UserRole.SEEKER) ?: return@get
            call.respond(seekerProfileService.listEducation(user.id))
        }
        post("/education") {
            val user = roleGuard.requireRole(call, UserRole.SEEKER) ?: return@post
            val body = call.receive<CreateSeekerEducationRequest>()
            call.respond(HttpStatusCode.Created, seekerProfileService.createEducation(user.id, body))
        }
        patch("/education/{id}") {
            val user = roleGuard.requireRole(call, UserRole.SEEKER) ?: return@patch
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@patch call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Некорректный id"))
            val body = call.receive<UpdateSeekerEducationRequest>()
            try {
                call.respond(seekerProfileService.updateEducation(user.id, id, body))
            } catch (e: IllegalStateException) {
                call.respond(HttpStatusCode.NotFound, mapOf("message" to (e.message ?: "Не найдено")))
            }
        }
        delete("/education/{id}") {
            val user = roleGuard.requireRole(call, UserRole.SEEKER) ?: return@delete
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Некорректный id"))
            try {
                seekerProfileService.deleteEducation(user.id, id)
                call.respond(HttpStatusCode.NoContent)
            } catch (e: IllegalStateException) {
                call.respond(HttpStatusCode.NotFound, mapOf("message" to (e.message ?: "Не найдено")))
            }
        }
        get("/skills") {
            val user = roleGuard.requireRole(call, UserRole.SEEKER) ?: return@get
            call.respond(seekerProfileService.getSkills(user.id))
        }
        put("/skills") {
            val user = roleGuard.requireRole(call, UserRole.SEEKER) ?: return@put
            val body = call.receive<UpdateSeekerSkillsRequest>()
            call.respond(seekerProfileService.setSkills(user.id, body.skillIds))
        }
        get("/desired-positions") {
            val user = roleGuard.requireRole(call, UserRole.SEEKER) ?: return@get
            call.respond(seekerProfileService.getDesiredPositions(user.id))
        }
        put("/desired-positions") {
            val user = roleGuard.requireRole(call, UserRole.SEEKER) ?: return@put
            val body = call.receive<UpdateSeekerDesiredPositionsRequest>()
            try {
                call.respond(seekerProfileService.setDesiredPositions(user.id, body.occupationIds))
            } catch (e: IllegalStateException) {
                call.respond(HttpStatusCode.BadRequest, mapOf("message" to (e.message ?: "Ошибка")))
            }
        }
        get("/personality-preview") {
            val user = roleGuard.requireRole(call, UserRole.SEEKER) ?: return@get
            call.respond(seekerProfileService.personalityPreview(user.id))
        }
        post("/personality/generate") {
            val user = roleGuard.requireRole(call, UserRole.SEEKER) ?: return@post
            try {
                personalityProfileService.triggerGeneration(user.id)
                call.respond(mapOf("status" to "PROCESSING"))
            } catch (e: IllegalStateException) {
                call.respond(HttpStatusCode.Conflict, mapOf("message" to (e.message ?: "Генерация уже выполняется")))
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, mapOf("message" to (e.message ?: "Ошибка")))
            }
        }
        get("/personality/llm-context") {
            val user = roleGuard.requireRole(call, UserRole.SEEKER) ?: return@get
            call.respond(surveyService.buildLlmContext(user.id))
        }
        get("/surveys") {
            val user = roleGuard.requireRole(call, UserRole.SEEKER) ?: return@get
            call.respond(surveyService.listGroups(user.id))
        }
        get("/surveys/{id}") {
            val user = roleGuard.requireRole(call, UserRole.SEEKER) ?: return@get
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Некорректный id"))
            try {
                call.respond(surveyService.getSurvey(user.id, id))
            } catch (e: IllegalStateException) {
                call.respond(HttpStatusCode.NotFound, mapOf("message" to (e.message ?: "Не найдено")))
            }
        }
        post("/surveys/{id}/start") {
            val user = roleGuard.requireRole(call, UserRole.SEEKER) ?: return@post
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Некорректный id"))
            try {
                call.respond(surveyService.startSurvey(user.id, id))
            } catch (e: IllegalStateException) {
                val status = if (e.message?.contains("пройден") == true) HttpStatusCode.Conflict else HttpStatusCode.BadRequest
                call.respond(status, mapOf("message" to (e.message ?: "Ошибка")))
            }
        }
        put("/surveys/{id}/answers") {
            val user = roleGuard.requireRole(call, UserRole.SEEKER) ?: return@put
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Некорректный id"))
            val body = call.receive<SaveSurveyAnswersRequest>()
            try {
                call.respond(surveyService.saveAnswers(user.id, id, body))
            } catch (e: IllegalStateException) {
                call.respond(HttpStatusCode.BadRequest, mapOf("message" to (e.message ?: "Ошибка")))
            }
        }
        post("/surveys/{id}/complete") {
            val user = roleGuard.requireRole(call, UserRole.SEEKER) ?: return@post
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Некорректный id"))
            val body = call.receive<SaveSurveyAnswersRequest>()
            try {
                val result = surveyService.completeSurvey(user.id, id, body)
                call.respond(
                    jobs.procrush.survey.CompleteSurveyResponseDto(
                        resultId = result.result.id,
                        surveyId = result.result.surveyId,
                        status = jobs.procrush.survey.SurveyStatus.COMPLETED,
                        nextSurveyId = result.nextSurveyId,
                    ),
                )
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, mapOf("message" to (e.message ?: "Некорректные ответы")))
            } catch (e: IllegalStateException) {
                val status = if (e.message?.contains("пройден") == true) HttpStatusCode.Conflict else HttpStatusCode.BadRequest
                call.respond(status, mapOf("message" to (e.message ?: "Ошибка")))
            }
        }
        get("/recommendations") {
            roleGuard.requireRole(call, UserRole.SEEKER) ?: return@get
            call.respond(seekerProfileService.recommendations())
        }
    }
}
