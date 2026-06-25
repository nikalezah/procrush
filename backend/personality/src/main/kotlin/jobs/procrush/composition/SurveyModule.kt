package jobs.procrush.composition

import jobs.procrush.personality.port.PersonalitySurveyCoordinator
import java.util.UUID

private object NoOpPersonalitySurveyCoordinator : PersonalitySurveyCoordinator {
    override fun onAllSurveysCompleted(userId: UUID) = Unit
}

data class SurveyModule(
    val surveyService: jobs.procrush.survey.service.SurveyService,
) {
    companion object {
        fun create(auth: AuthModule): SurveyModule {
            val surveyRepository = jobs.procrush.survey.repository.SurveyRepository()
            val surveyService =
                jobs.procrush.survey.service.SurveyService(
                    auth.seekerRepository,
                    surveyRepository,
                    NoOpPersonalitySurveyCoordinator,
                )
            return SurveyModule(surveyService = surveyService)
        }
    }
}
