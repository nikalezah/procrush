package jobs.procrush.bootstrap.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import jobs.procrush.shared.GenerationInProgressException
import jobs.procrush.shared.RegistrationConflictException
import jobs.procrush.shared.ResourceNotFoundException
import jobs.procrush.shared.SurveyAlreadyCompletedException

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<IllegalArgumentException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, mapOf("message" to (cause.message ?: "Некорректные данные")))
        }
        exception<SurveyAlreadyCompletedException> { call, cause ->
            call.respond(HttpStatusCode.Conflict, mapOf("message" to (cause.message ?: "Опрос уже пройден")))
        }
        exception<GenerationInProgressException> { call, cause ->
            call.respond(HttpStatusCode.Conflict, mapOf("message" to (cause.message ?: "Генерация уже выполняется")))
        }
        exception<RegistrationConflictException> { call, cause ->
            call.respond(HttpStatusCode.Conflict, mapOf("message" to (cause.message ?: "Пользователь уже зарегистрирован")))
        }
        exception<ResourceNotFoundException> { call, cause ->
            call.respond(HttpStatusCode.NotFound, mapOf("message" to (cause.message ?: "Не найдено")))
        }
        exception<IllegalStateException> { call, cause ->
            call.respond(HttpStatusCode.NotFound, mapOf("message" to (cause.message ?: "Не найдено")))
        }
    }
}
