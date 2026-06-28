package jobs.procrush.api.handler

import io.ktor.server.application.ApplicationCall
import jobs.procrush.api.generated.survey_models_yaml.survey_models.SaveSurveyAnswersRequest
import jobs.procrush.api.generated.survey_paths_yaml.survey_paths.SeekerSurveyServerApi
import jobs.procrush.api.mapper.toApi
import jobs.procrush.api.mapper.toContract
import jobs.procrush.auth.service.RoleGuard
import jobs.procrush.i18n.ErrorCode
import jobs.procrush.personality.service.PersonalityProfileService
import jobs.procrush.shared.CodedException
import jobs.procrush.shared.ResourceNotFoundException
import jobs.procrush.shared.SurveyAlreadyCompletedException
import jobs.procrush.survey.dto.SurveyStatus
import jobs.procrush.survey.service.SurveyService

class SeekerSurveyHandler(
    private val roleGuard: RoleGuard,
    private val surveyService: SurveyService,
    private val personalityProfileService: PersonalityProfileService,
) : SeekerSurveyServerApi {
    override suspend fun getSurveyLlmContext(call: ApplicationCall): SeekerSurveyServerApi.GetSurveyLlmContextResponse =
        roleGuard.withSeeker(
            call,
            SeekerSurveyServerApi.GetSurveyLlmContextResponse::unauthorized,
            SeekerSurveyServerApi.GetSurveyLlmContextResponse::forbidden,
        ) { user ->
            SeekerSurveyServerApi.GetSurveyLlmContextResponse.ok(
                personalityProfileService.buildLlmContext(user.id).toApi(),
            )
        }

    override suspend fun listSurveys(call: ApplicationCall): SeekerSurveyServerApi.ListSurveysResponse =
        roleGuard.withSeeker(
            call,
            SeekerSurveyServerApi.ListSurveysResponse::unauthorized,
            SeekerSurveyServerApi.ListSurveysResponse::forbidden,
        ) { user ->
            SeekerSurveyServerApi.ListSurveysResponse.ok(surveyService.listGroups(user.id).toApi())
        }

    override suspend fun getSurvey(
        id: Long,
        call: ApplicationCall,
    ): SeekerSurveyServerApi.GetSurveyResponse =
        roleGuard.withSeeker(
            call,
            SeekerSurveyServerApi.GetSurveyResponse::unauthorized,
            SeekerSurveyServerApi.GetSurveyResponse::forbidden,
        ) { user ->
            try {
                SeekerSurveyServerApi.GetSurveyResponse.ok(surveyService.getSurvey(user.id, id).toApi())
            } catch (_: ResourceNotFoundException) {
                SeekerSurveyServerApi.GetSurveyResponse.notFound(errorNotFound())
            } catch (_: CodedException) {
                SeekerSurveyServerApi.GetSurveyResponse.notFound(errorNotFound())
            }
        }

    override suspend fun startSurvey(
        id: Long,
        call: ApplicationCall,
    ): SeekerSurveyServerApi.StartSurveyResponse =
        roleGuard.withSeeker(
            call,
            SeekerSurveyServerApi.StartSurveyResponse::unauthorized,
            SeekerSurveyServerApi.StartSurveyResponse::forbidden,
        ) { user ->
            try {
                SeekerSurveyServerApi.StartSurveyResponse.ok(surveyService.startSurvey(user.id, id).toApi())
            } catch (_: ResourceNotFoundException) {
                SeekerSurveyServerApi.StartSurveyResponse.notFound(errorNotFound())
            }
        }

    override suspend fun saveSurveyAnswers(
        request: SaveSurveyAnswersRequest,
        id: Long,
        call: ApplicationCall,
    ): SeekerSurveyServerApi.SaveSurveyAnswersResponse =
        roleGuard.withSeeker(
            call,
            SeekerSurveyServerApi.SaveSurveyAnswersResponse::unauthorized,
            SeekerSurveyServerApi.SaveSurveyAnswersResponse::forbidden,
        ) { user ->
            try {
                SeekerSurveyServerApi.SaveSurveyAnswersResponse.ok(
                    surveyService.saveAnswers(user.id, id, request.toContract()).toApi(),
                )
            } catch (e: CodedException) {
                SeekerSurveyServerApi.SaveSurveyAnswersResponse.badRequest(errorBadRequest(e.errorCode, e.details))
            } catch (_: ResourceNotFoundException) {
                SeekerSurveyServerApi.SaveSurveyAnswersResponse.notFound(errorNotFound())
            } catch (_: SurveyAlreadyCompletedException) {
                SeekerSurveyServerApi.SaveSurveyAnswersResponse.conflict(errorConflict(ErrorCode.SURVEY_ALREADY_COMPLETED))
            }
        }

    override suspend fun completeSurvey(
        request: SaveSurveyAnswersRequest,
        id: Long,
        call: ApplicationCall,
    ): SeekerSurveyServerApi.CompleteSurveyResponse =
        roleGuard.withSeeker(
            call,
            SeekerSurveyServerApi.CompleteSurveyResponse::unauthorized,
            SeekerSurveyServerApi.CompleteSurveyResponse::forbidden,
        ) { user ->
            try {
                val result = surveyService.completeSurvey(user.id, id, request.toContract())
                SeekerSurveyServerApi.CompleteSurveyResponse.ok(
                    jobs.procrush.survey.dto.CompleteSurveyResponseDto(
                        resultId = result.result.id,
                        surveyId = result.result.surveyId,
                        status = SurveyStatus.COMPLETED,
                        nextSurveyId = result.nextSurveyId,
                    ).toApi(),
                )
            } catch (e: CodedException) {
                SeekerSurveyServerApi.CompleteSurveyResponse.badRequest(errorBadRequest(e.errorCode, e.details))
            } catch (_: ResourceNotFoundException) {
                SeekerSurveyServerApi.CompleteSurveyResponse.notFound(errorNotFound())
            } catch (_: SurveyAlreadyCompletedException) {
                SeekerSurveyServerApi.CompleteSurveyResponse.conflict(errorConflict(ErrorCode.SURVEY_ALREADY_COMPLETED))
            }
        }
}
