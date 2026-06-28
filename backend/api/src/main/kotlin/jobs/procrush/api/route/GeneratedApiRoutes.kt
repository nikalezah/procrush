package jobs.procrush.api.route

import io.ktor.server.routing.Route
import jobs.procrush.api.generated.auth_paths_yaml.auth_paths.AuthRoutes
import jobs.procrush.api.generated.employer_paths_yaml.employer_paths.EmployerRoutes
import jobs.procrush.api.generated.personality_paths_yaml.personality_paths.SeekerPersonalityRoutes
import jobs.procrush.api.generated.reference_paths_yaml.reference_paths.ReferenceRoutes
import jobs.procrush.api.generated.seeker_paths_yaml.seeker_paths.SeekerProfileRoutes
import jobs.procrush.api.generated.survey_paths_yaml.survey_paths.SeekerSurveyRoutes
import jobs.procrush.api.handler.ApiHandlers

fun Route.generatedApiRoutes(handlers: ApiHandlers) {
    with(AuthRoutes(handlers.auth)) {
        devLogin()
        getMe()
        logout()
        completeRegistration()
        deleteAccount()
    }
    with(ReferenceRoutes(handlers.reference)) {
        listOccupations()
        searchSkills()
    }
    with(SeekerProfileRoutes(handlers.seekerProfile)) {
        getSeekerDashboard()
        getSeekerProfile()
        updateSeekerProfile()
        listSeekerExperience()
        createSeekerExperience()
        deleteSeekerExperience()
        updateSeekerExperience()
        listSeekerEducation()
        createSeekerEducation()
        deleteSeekerEducation()
        updateSeekerEducation()
        getSeekerSkills()
        setSeekerSkills()
        getSeekerDesiredPositions()
        setSeekerDesiredPositions()
        getSeekerPositionsOverview()
        getSeekerRecommendations()
        seekerRespondToJob()
        getSeekerInterests()
        getSeekerMatchInterestCount()
    }
    with(SeekerSurveyRoutes(handlers.seekerSurvey)) {
        getSurveyLlmContext()
        listSurveys()
        getSurvey()
        startSurvey()
        saveSurveyAnswers()
        completeSurvey()
    }
    with(SeekerPersonalityRoutes(handlers.seekerPersonality)) {
        getPersonalityPreview()
        triggerPersonalityGeneration()
    }
    with(EmployerRoutes(handlers.employer)) {
        getEmployerDashboard()
        getEmployerProfile()
        updateEmployerProfile()
        listJobProfiles()
        createJobProfile()
        deleteJobProfile()
        updateJobProfile()
        getJobCandidates()
        getJobCandidatesOverview()
        employerRespondToCandidate()
        getEmployerInterests()
        getEmployerMatchInterestCount()
    }
}
