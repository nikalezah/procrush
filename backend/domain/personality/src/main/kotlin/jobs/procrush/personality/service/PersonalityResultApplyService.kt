package jobs.procrush.personality.service

import jobs.procrush.i18n.ErrorCode
import jobs.procrush.matching.port.MatchingCachePort
import jobs.procrush.matching.port.MatchingEventPort
import jobs.procrush.personality.dto.PersonalityAxesDto
import jobs.procrush.personality.dto.PersonalityProfileStatus
import jobs.procrush.personality.llm.PersonalityProfileLlmMapper
import jobs.procrush.personality.messaging.PersonalityGenerationResult
import jobs.procrush.personality.messaging.PersonalityGenerationResultStatus
import jobs.procrush.seeker.repository.SeekerPersonalProfileRepository
import jobs.procrush.shared.raise
import jobs.procrush.shared.repository.ReferenceRepository
import org.slf4j.LoggerFactory
import java.util.UUID

class PersonalityResultApplyService(
    private val profileRepository: SeekerPersonalProfileRepository,
    private val referenceRepository: ReferenceRepository,
    private val lockGuard: PersonalityGenerationLockGuard,
    private val statusNotifier: RedisPersonalityStatusNotifier,
    private val matchingCache: MatchingCachePort,
    private val matchingEvents: MatchingEventPort,
) {
    private val logger = LoggerFactory.getLogger(PersonalityResultApplyService::class.java)

    fun apply(result: PersonalityGenerationResult) {
        val userId =
            runCatching { UUID.fromString(result.userId) }.getOrElse {
                ErrorCode.UNKNOWN_ERROR.raise()
            }
        try {
            when (result.status) {
                PersonalityGenerationResultStatus.SUCCESS -> applySuccess(result, userId)
                PersonalityGenerationResultStatus.FAILED -> applyFailure(result, userId)
            }
        } finally {
            lockGuard.release(result.seekerId)
        }
    }

    private fun applySuccess(
        result: PersonalityGenerationResult,
        userId: UUID,
    ) {
        val existing = profileRepository.findBySeekerId(result.seekerId)
        if (existing?.generationStatus == PersonalityProfileStatus.READY) {
            logger.info(
                "Personality profile already READY for seekerId={}, skipping upsert",
                result.seekerId,
            )
            statusNotifier.notify(userId, PersonalityProfileStatus.READY)
            return
        }

        val profile =
            result.profile
                ?: ErrorCode.LLM_OUTPUT_FIELD_REQUIRED.raise(mapOf("field" to "profile"))
        val record = PersonalityProfileLlmMapper.fromLlmOutput(result.seekerId, profile)
        val nameToId =
            referenceRepository.findSuperpowersAndTalentsByNames(
                profile.superpowersAndTalents.map { it.name },
            )
        val superpowerRows =
            profile.superpowersAndTalents.map { item ->
                nameToId.getValue(item.name) to item.isPronounced
            }
        profileRepository.upsertProfileWithSuperpowers(result.seekerId, record, superpowerRows)
        matchingCache.invalidateSeekerJobs(result.seekerId)
        PersonalityAxesDto.fromSeekerRecord(record)?.let { axes ->
            matchingEvents.publishSeekerPersonalityReady(result.seekerId, axes)
        }
        statusNotifier.notify(userId, PersonalityProfileStatus.READY)
        logger.info("Applied personality SUCCESS result seekerId={}", result.seekerId)
    }

    private fun applyFailure(
        result: PersonalityGenerationResult,
        userId: UUID,
    ) {
        val errorCode = result.errorCode ?: ErrorCode.UNKNOWN_ERROR.name
        profileRepository.markFailed(result.seekerId, errorCode)
        statusNotifier.notify(userId, PersonalityProfileStatus.FAILED)
        logger.info(
            "Applied personality FAILED result seekerId={} errorCode={}",
            result.seekerId,
            errorCode,
        )
    }
}
