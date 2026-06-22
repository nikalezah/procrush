package jobs.procrush.personality.service

import io.ktor.client.plugins.HttpRequestTimeoutException
import jobs.procrush.bootstrap.config.LlmConfig
import jobs.procrush.bootstrap.config.RedisConfig
import jobs.procrush.bootstrap.redis.RedisDistributedLock
import jobs.procrush.llm.LlmClient
import jobs.procrush.matching.cache.MatchingCacheInvalidator
import jobs.procrush.personality.dto.PersonalityProfileStatus
import jobs.procrush.personality.dto.SeekerPersonalProfileRecord
import jobs.procrush.personality.llm.PersonalityProfileMapper
import jobs.procrush.personality.llm.PersonalityProfileValidator
import jobs.procrush.personality.llm.PersonalityPromptBuilder
import jobs.procrush.seeker.repository.SeekerPersonalProfileRepository
import jobs.procrush.seeker.repository.SeekerRepository
import jobs.procrush.shared.repository.ReferenceRepository
import jobs.procrush.survey.service.SurveyService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.UUID

class PersonalityProfileGenerator(
    private val seekerRepository: SeekerRepository,
    private val profileRepository: SeekerPersonalProfileRepository,
    private val referenceRepository: ReferenceRepository,
    private val surveyService: SurveyService,
    private val llmConfig: LlmConfig,
    private val llmClient: LlmClient,
    private val promptBuilder: PersonalityPromptBuilder,
    private val validator: PersonalityProfileValidator,
    private val notifier: PersonalityGenerationNotifier,
    private val distributedLock: RedisDistributedLock,
    private val redisConfig: RedisConfig,
    private val matchingCacheInvalidator: MatchingCacheInvalidator,
    private val scope: CoroutineScope,
) {
    private val logger = LoggerFactory.getLogger(PersonalityProfileGenerator::class.java)

    fun isJobActive(seekerId: Long): Boolean =
        distributedLock.isHeld(personalityLockKey(seekerId))

    fun isStale(record: SeekerPersonalProfileRecord): Boolean =
        record.generationStatus == PersonalityProfileStatus.PROCESSING &&
            !isJobActive(record.seekerId)

    fun startGeneration(seekerId: Long, userId: UUID) {
        val lockKey = personalityLockKey(seekerId)
        val lockHandle =
            distributedLock.tryAcquire(lockKey, redisConfig.personalityLockTtlSeconds)
                ?: return

        profileRepository.markProcessing(seekerId)

        scope.launch {
            try {
                generateProfile(seekerId, userId)
            } catch (e: Exception) {
                logger.error("Personality profile generation failed for seeker $seekerId", e)
                val message =
                    when (e) {
                        is HttpRequestTimeoutException ->
                            "Превышено время ожидания ответа LLM (${llmConfig.requestTimeoutSeconds} с). " +
                                "Попробуйте ещё раз или увеличьте LLM_REQUEST_TIMEOUT_SECONDS."
                        else -> e.message ?: "Неизвестная ошибка"
                    }
                profileRepository.markFailed(seekerId, message)
                notifier.notify(userId, PersonalityProfileStatus.FAILED)
            } finally {
                distributedLock.release(lockHandle)
            }
        }
    }

    private suspend fun generateProfile(seekerId: Long, userId: UUID) {
        val context = surveyService.buildLlmContext(userId)
        require(context.surveys.isNotEmpty()) { "Нет завершённых опросов для интерпретации" }

        val catalog = referenceRepository.listSuperpowersAndTalents()
        val catalogNames = catalog.map { it.name }.toSet()
        val (systemPrompt, userPrompt) = promptBuilder.build(context, catalog)
        val rawResponse = llmClient.chat(systemPrompt, userPrompt)
        val output = validator.validateAndParse(rawResponse, catalogNames)
        val record = PersonalityProfileMapper.fromLlmOutput(seekerId, output)
        val nameToId = referenceRepository.findSuperpowersAndTalentsByNames(output.superpowersAndTalents.map { it.name })
        val superpowerRows =
            output.superpowersAndTalents.map { item ->
                nameToId.getValue(item.name) to item.isPronounced
            }
        profileRepository.upsertProfileWithSuperpowers(seekerId, record, superpowerRows)
        matchingCacheInvalidator.invalidateSeekerJobs(seekerId)
        notifier.notify(userId, PersonalityProfileStatus.READY)
    }

    private fun personalityLockKey(seekerId: Long): String =
        redisConfig.key("lock", "personality", seekerId.toString())
}
