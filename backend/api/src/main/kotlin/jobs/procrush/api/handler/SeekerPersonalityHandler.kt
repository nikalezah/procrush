package jobs.procrush.api.handler

import io.ktor.server.application.ApplicationCall
import jobs.procrush.api.generated.personality_paths_yaml.personality_paths.SeekerPersonalityServerApi
import jobs.procrush.api.mapper.toApi
import jobs.procrush.api.mapper.toGenerationStatusResponse
import jobs.procrush.auth.service.RoleGuard
import jobs.procrush.i18n.ErrorCode
import jobs.procrush.personality.dto.PersonalityProfileStatus
import jobs.procrush.personality.service.PersonalityProfileService
import jobs.procrush.shared.GenerationInProgressException

class SeekerPersonalityHandler(
    private val roleGuard: RoleGuard,
    private val personalityProfileService: PersonalityProfileService,
) : SeekerPersonalityServerApi {
    override suspend fun getPersonalityPreview(call: ApplicationCall): SeekerPersonalityServerApi.GetPersonalityPreviewResponse =
        roleGuard.withSeeker(
            call,
            SeekerPersonalityServerApi.GetPersonalityPreviewResponse::unauthorized,
            SeekerPersonalityServerApi.GetPersonalityPreviewResponse::forbidden,
        ) { user ->
            SeekerPersonalityServerApi.GetPersonalityPreviewResponse.ok(
                personalityProfileService.getPreview(user.id).toApi(),
            )
        }

    override suspend fun triggerPersonalityGeneration(
        call: ApplicationCall,
    ): SeekerPersonalityServerApi.TriggerPersonalityGenerationResponse =
        roleGuard.withSeeker(
            call,
            SeekerPersonalityServerApi.TriggerPersonalityGenerationResponse::unauthorized,
            SeekerPersonalityServerApi.TriggerPersonalityGenerationResponse::forbidden,
        ) { user ->
            try {
                personalityProfileService.triggerGeneration(user.id)
                SeekerPersonalityServerApi.TriggerPersonalityGenerationResponse.ok(
                    PersonalityProfileStatus.PROCESSING.toGenerationStatusResponse(),
                )
            } catch (_: GenerationInProgressException) {
                SeekerPersonalityServerApi.TriggerPersonalityGenerationResponse.conflict(
                    errorConflict(ErrorCode.GENERATION_IN_PROGRESS),
                )
            }
        }
}
