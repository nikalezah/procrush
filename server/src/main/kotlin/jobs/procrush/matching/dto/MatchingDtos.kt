package jobs.procrush.matching.dto

import kotlinx.serialization.Serializable

@Serializable
data class JobRecommendationDto(
    val id: Long,
    val companyName: String,
    val positionName: String,
    val description: String,
    val matchScore: Double,
    val matchScoreDisplay: Int,
)

@Serializable
data class CandidateRecommendationDto(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val positionName: String,
    val skills: List<String>,
    val matchScore: Double,
    val matchScoreDisplay: Int,
)
