package jobs.procrush.shared.dto

import kotlinx.serialization.Serializable

@Serializable
data class OccupationDto(
    val id: Long,
    val parentId: Long?,
    val name: String,
    val isLeaf: Boolean,
)

@Serializable
data class SkillDto(
    val id: Long,
    val name: String,
)

@Serializable
data class SuperpowerAndTalentDto(
    val id: Long,
    val name: String,
    val isPronounced: Boolean? = null,
)
