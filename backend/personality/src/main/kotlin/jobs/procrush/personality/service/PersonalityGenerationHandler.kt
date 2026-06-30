package jobs.procrush.personality.service

import io.ktor.client.plugins.HttpRequestTimeoutException
import jobs.procrush.bootstrap.config.LlmConfig
import jobs.procrush.i18n.ErrorCode
import jobs.procrush.llm.LlmClient
import jobs.procrush.matching.port.MatchingCachePort
import jobs.procrush.matching.port.MatchingEventPort
import jobs.procrush.observability.AppMetrics
import jobs.procrush.observability.ObservabilityHolder
import jobs.procrush.personality.dto.PersonalityAxesDto
import jobs.procrush.personality.dto.PersonalityProfileStatus
import jobs.procrush.personality.llm.PersonalityProfileLlmMapper
import jobs.procrush.personality.llm.PersonalityProfileValidator
import jobs.procrush.personality.llm.PersonalityPromptBuilder
import jobs.procrush.seeker.repository.SeekerPersonalProfileRepository
import jobs.procrush.shared.raise
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
    private val profileMapper: PersonalityProfileLlmMapper,
    private val matchingCache: MatchingCachePort,
    private val matchingEvents: MatchingEventPort,
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
        if (context.surveys.isEmpty()) {
            ErrorCode.NO_COMPLETED_SURVEYS.raise()
        }

        val catalog = referenceRepository.listSuperpowersAndTalents()
        val catalogNames = catalog.map { it.name }.toSet()
        val (systemPrompt, userPrompt) = promptBuilder.build(context, catalog)
        val rawResponse =
            AppMetrics.recordPersonalityLlm {
                ObservabilityHolder.tracing.suspendSpan("llm.chat") {
                    llmClient.chat(systemPrompt, userPrompt)
                }
            }
        val output = validator.validateAndParse(rawResponse, catalogNames)
        val record = profileMapper.fromLlmOutput(seekerId, output)
        val nameToId = referenceRepository.findSuperpowersAndTalentsByNames(output.superpowersAndTalents.map { it.name })
        val superpowerRows =
            output.superpowersAndTalents.map { item ->
                nameToId.getValue(item.name) to item.isPronounced
            }
        profileRepository.upsertProfileWithSuperpowers(seekerId, record, superpowerRows)
        matchingCache.invalidateSeekerJobs(seekerId)
        PersonalityAxesDto.fromSeekerRecord(record)?.let { axes ->
            matchingEvents.publishSeekerPersonalityReady(seekerId, axes)
        }
    }

    fun failureCode(error: Throwable): String =
        when (error) {
            is HttpRequestTimeoutException -> ErrorCode.LLM_TIMEOUT.name
            is jobs.procrush.shared.CodedException -> error.errorCode.name
            else -> ErrorCode.UNKNOWN_ERROR.name
        }
}
