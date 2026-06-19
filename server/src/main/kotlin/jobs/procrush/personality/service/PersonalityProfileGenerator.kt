package jobs.procrush.personality.service

import io.ktor.client.plugins.HttpRequestTimeoutException
import jobs.procrush.bootstrap.config.LlmConfig
import jobs.procrush.llm.LlmClient
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

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
                        notifier.notify(userId, PersonalityProfileStatus.FAILED)
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
        notifier.notify(userId, PersonalityProfileStatus.READY)
    }
}
