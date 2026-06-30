package jobs.procrush.matching.events

import jobs.procrush.personality.dto.PersonalityAxesDto
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement

object MatchingEventTypes {
    const val SEEKER_PROFILE_CHANGED = "seeker.profile_changed"
    const val SEEKER_PERSONALITY_READY = "seeker.personality_ready"
    const val JOB_PROFILE_CHANGED = "job_profile.changed"
}

@Serializable
data class MatchingEventEnvelope(
    val eventId: String,
    val eventType: String,
    val occurredAt: String,
    val payload: JsonElement,
    val correlationId: String? = null,
)

@Serializable
data class SeekerProfileChangedPayload(
    val seekerId: Long,
    val desiredOccupationIds: List<Long>,
    val skillIds: List<Long>,
    val personalityReady: Boolean,
    val personalityAxes: PersonalityAxesDto? = null,
    val firstName: String = "",
    val lastName: String = "",
    val skillNames: List<String> = emptyList(),
    val matchingEligible: Boolean = false,
)

@Serializable
data class SeekerPersonalityReadyPayload(
    val seekerId: Long,
    val desiredOccupationIds: List<Long>,
    val skillIds: List<Long>,
    val personalityAxes: PersonalityAxesDto,
    val firstName: String = "",
    val lastName: String = "",
    val skillNames: List<String> = emptyList(),
    val matchingEligible: Boolean = true,
)

@Serializable
data class JobProfileChangedPayload(
    val jobProfileId: Long,
    val occupationId: Long,
    val skillIds: List<Long>,
    val personalityAxes: PersonalityAxesDto,
    val isActive: Boolean,
    val companyName: String? = null,
    val occupationName: String,
    val description: String? = null,
    val deleted: Boolean = false,
)

object MatchingEventJson {
    val json: Json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    inline fun <reified T> decodePayload(envelope: MatchingEventEnvelope): T =
        json.decodeFromJsonElement(envelope.payload)
}
