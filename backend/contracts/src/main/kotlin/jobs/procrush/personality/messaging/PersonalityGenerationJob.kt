package jobs.procrush.personality.messaging

import kotlinx.serialization.Serializable

@Serializable
data class PersonalityGenerationJob(
    val seekerId: Long,
    val userId: String,
    val enqueuedAt: String,
    val attempt: Int = 1,
)
