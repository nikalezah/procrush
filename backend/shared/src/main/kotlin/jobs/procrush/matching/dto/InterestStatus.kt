package jobs.procrush.matching.dto

import kotlinx.serialization.Serializable

@Serializable
enum class InterestStatus {
    NONE,
    RESPONDED,
    INCOMING,
    MUTUAL,
}
