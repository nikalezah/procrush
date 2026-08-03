package jobs.procrush.matching.dto

import kotlinx.serialization.Serializable

@Serializable
data class RecommendationsUpdatedEventDto(
    val scope: String,
    val id: Long,
    val computedAt: String,
)
