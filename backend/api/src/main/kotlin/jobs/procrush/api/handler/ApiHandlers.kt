package jobs.procrush.api.handler

data class ApiHandlers(
    val auth: AuthHandler,
    val reference: ReferenceHandler,
    val seekerProfile: SeekerProfileHandler,
    val seekerSurvey: SeekerSurveyHandler,
    val seekerPersonality: SeekerPersonalityHandler,
    val employer: EmployerHandler,
)
