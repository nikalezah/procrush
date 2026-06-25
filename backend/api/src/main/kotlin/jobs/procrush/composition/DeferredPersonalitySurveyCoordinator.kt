package jobs.procrush.composition

import jobs.procrush.personality.port.PersonalitySurveyCoordinator
import java.util.UUID

internal class DeferredPersonalitySurveyCoordinator : PersonalitySurveyCoordinator {
    private var delegate: PersonalitySurveyCoordinator? = null

    fun bind(coordinator: PersonalitySurveyCoordinator) {
        delegate = coordinator
    }

    override fun onAllSurveysCompleted(userId: UUID) {
        delegate?.onAllSurveysCompleted(userId)
    }
}
