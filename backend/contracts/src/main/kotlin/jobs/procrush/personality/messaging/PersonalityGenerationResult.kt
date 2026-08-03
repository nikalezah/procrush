package jobs.procrush.personality.messaging

import jobs.procrush.personality.dto.SeekerPersonalProfileLlmOutput
import kotlinx.serialization.Serializable

@Serializable
enum class PersonalityGenerationResultStatus {
    SUCCESS,
    FAILED,
}

@Serializable
data class PersonalityGenerationResult(
    val seekerId: Long,
    val userId: String,
    val status: PersonalityGenerationResultStatus,
    val attempt: Int = 1,
    val correlationId: String? = null,
    val commandMessageId: String? = null,
    val profile: SeekerPersonalProfileLlmOutput? = null,
    val errorCode: String? = null,
)
