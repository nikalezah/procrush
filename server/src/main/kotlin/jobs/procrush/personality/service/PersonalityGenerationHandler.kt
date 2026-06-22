package jobs.procrush.personality.service

import io.ktor.client.plugins.HttpRequestTimeoutException
import jobs.procrush.bootstrap.config.LlmConfig
import jobs.procrush.llm.LlmClient
import jobs.procrush.matching.cache.MatchingCacheInvalidator
import jobs.procrush.personality.dto.PersonalityProfileStatus
import jobs.procrush.personality.llm.PersonalityProfileMapper
import jobs.procrush.personality.llm.PersonalityProfileValidator
import jobs.procrush.personality.llm.PersonalityPromptBuilder
import jobs.procrush.seeker.repository.SeekerPersonalProfileRepository
import jobs.procrush.shared.repository.ReferenceRepository
import jobs.procrush.survey.service.SurveyService
import org.slf4j.LoggerFactory
import java.util.UUID

class PersonalityGenerationHandler(
    private val profileRepository: SeekerPersonalProfileRepository,
    private val referenceRepository: ReferenceRepository,
    private val surveyService: SurveyService,
    private val llmConfig: LlmConfig,
    private val llmClient: LlmClient,
    private val promptBuilder: PersonalityPromptBuilder,
    private val validator: PersonalityProfileValidator,
    private val matchingCacheInvalidator: MatchingCacheInvalidator,
) {
    private val logger = LoggerFactory.getLogger(PersonalityGenerationHandler::class.java)

    fun isAlreadyReady(seekerId: Long): Boolean {
        val record = profileRepository.findBySeekerId(seekerId) ?: return false
        return record.generationStatus == PersonalityProfileStatus.READY
    }

    suspend fun generate(seekerId: Long, userId: UUID) {
        llmConfig.validateForGeneration()
        if (isAlreadyReady(seekerId)) {
            logger.info("Personality profile already READY for seeker {}, skipping LLM", seekerId)
            return
        }

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
    }

    fun failureMessage(error: Throwable): String =
        when (error) {
            is HttpRequestTimeoutException ->
                "Превышено время ожидания ответа LLM (${llmConfig.requestTimeoutSeconds} с). " +
                    "Попробуйте ещё раз или увеличьте LLM_REQUEST_TIMEOUT_SECONDS."
            else -> error.message ?: "Неизвестная ошибка"
        }
}
