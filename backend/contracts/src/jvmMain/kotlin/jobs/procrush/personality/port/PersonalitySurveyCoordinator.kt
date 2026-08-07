package jobs.procrush.personality.port

import java.util.UUID

fun interface PersonalitySurveyCoordinator {
    fun onAllSurveysCompleted(userId: UUID)
}
