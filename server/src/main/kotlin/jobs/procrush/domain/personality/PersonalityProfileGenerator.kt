package jobs.procrush.domain.personality

import io.ktor.client.plugins.HttpRequestTimeoutException
import jobs.procrush.config.LlmConfig
import jobs.procrush.db.SeekerPersonalProfileRepository
import jobs.procrush.db.SeekerRepository
import jobs.procrush.domain.PersonalityProfileMapper
import jobs.procrush.domain.PersonalityProfileValidator
import jobs.procrush.domain.PersonalityPromptBuilder
import jobs.procrush.domain.SurveyService
import jobs.procrush.llm.LlmClient
import jobs.procrush.models.PersonalityProfileStatus
import jobs.procrush.models.SeekerPersonalProfileRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class PersonalityProfileGenerator(
    private val seekerRepository: SeekerRepository,
    private val profileRepository: SeekerPersonalProfileRepository,
    private val surveyService: SurveyService,
    private val llmConfig: LlmConfig,
    private val llmClient: LlmClient,
    private val promptBuilder: PersonalityPromptBuilder,
    private val validator: PersonalityProfileValidator,
    private val scope: CoroutineScope,
) {
    private val logger = LoggerFactory.getLogger(PersonalityProfileGenerator::class.java)
    private val activeJobs = ConcurrentHashMap<Long, Job>()
    private val generationLocks = ConcurrentHashMap<Long, Any>()

    fun isJobActive(seekerId: Long): Boolean = activeJobs[seekerId]?.isActive == true

    fun isStale(record: SeekerPersonalProfileRecord): Boolean =
        !isJobActive(record.seekerId) && record.generationStatus == PersonalityProfileStatus.PROCESSING

    fun startGeneration(seekerId: Long, userId: UUID) {
        val lock = generationLocks.computeIfAbsent(seekerId) { Any() }
        synchronized(lock) {
            if (activeJobs[seekerId]?.isActive == true) return

            profileRepository.markProcessing(seekerId)

            lateinit var job: Job
            job =
                scope.launch {
                    try {
                        generateProfile(seekerId, userId)
                    } catch (e: Exception) {
                        logger.error("Personality profile generation failed for seeker $seekerId", e)
                        if (activeJobs[seekerId] !== job) return@launch
                        val message =
                            when (e) {
                                is HttpRequestTimeoutException ->
                                    "Превышено время ожидания ответа LLM (${llmConfig.requestTimeoutSeconds} с). " +
                                        "Попробуйте ещё раз или увеличьте LLM_REQUEST_TIMEOUT_SECONDS."
                                else -> e.message ?: "Неизвестная ошибка"
                            }
                        profileRepository.markFailed(seekerId, message)
                    } finally {
                        activeJobs.remove(seekerId, job)
                    }
                }
            activeJobs[seekerId] = job
        }
    }

    private suspend fun generateProfile(seekerId: Long, userId: UUID) {
        val context = surveyService.buildLlmContext(userId)
        require(context.surveys.isNotEmpty()) { "Нет завершённых опросов для интерпретации" }

        val (systemPrompt, userPrompt) = promptBuilder.build(context)
        val rawResponse = llmClient.chat(systemPrompt, userPrompt)
        val output = validator.validateAndParse(rawResponse)
        val record = PersonalityProfileMapper.fromLlmOutput(seekerId, output)
        profileRepository.upsertProfile(seekerId, record)
    }
}
