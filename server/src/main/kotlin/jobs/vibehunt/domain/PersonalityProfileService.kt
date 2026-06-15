package jobs.procrush.domain

import io.ktor.client.plugins.HttpRequestTimeoutException
import jobs.procrush.config.LlmConfig
import jobs.procrush.db.SeekerPersonalProfileRepository
import jobs.procrush.db.SeekerRepository
import jobs.procrush.llm.LlmClient
import jobs.procrush.models.PersonalityPreviewDto
import jobs.procrush.models.PersonalityProfileStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class PersonalityProfileService(
    private val seekerRepository: SeekerRepository,
    private val profileRepository: SeekerPersonalProfileRepository,
    private val surveyService: SurveyService,
    private val llmConfig: LlmConfig,
    private val llmClient: LlmClient,
    private val promptBuilder: PersonalityPromptBuilder,
    private val validator: PersonalityProfileValidator,
    private val scope: CoroutineScope,
) {
    private val logger = LoggerFactory.getLogger(PersonalityProfileService::class.java)
    private val activeJobs = ConcurrentHashMap<Long, Job>()
    private val generationLocks = ConcurrentHashMap<Long, Any>()

    fun getPreview(userId: UUID): PersonalityPreviewDto {
        val groups = surveyService.listGroups(userId)
        val seeker = seekerRepository.findByUserId(userId) ?: error("Соискатель не найден")

        if (groups.testsCompleted < groups.testsTotal) {
            return PersonalityPreviewDto(
                status = PersonalityProfileStatus.NOT_READY,
                testsCompleted = groups.testsCompleted,
                testsTotal = groups.testsTotal,
            )
        }

        val processingPreview =
            PersonalityPreviewDto(
                status = PersonalityProfileStatus.PROCESSING,
                testsCompleted = groups.testsCompleted,
                testsTotal = groups.testsTotal,
            )

        if (activeJobs[seeker.id]?.isActive == true) {
            return processingPreview
        }

        maybeTriggerGeneration(userId)

        if (activeJobs[seeker.id]?.isActive == true) {
            return processingPreview
        }

        val current = profileRepository.findBySeekerId(seeker.id)
        if (current == null) {
            return processingPreview
        }

        return when (current.generationStatus) {
            PersonalityProfileStatus.READY ->
                PersonalityProfileMapper.toPreview(current, groups.testsCompleted, groups.testsTotal)
            PersonalityProfileStatus.FAILED ->
                PersonalityPreviewDto(
                    status = PersonalityProfileStatus.FAILED,
                    generationError = current.generationError ?: "Не удалось сформировать профиль",
                    testsCompleted = groups.testsCompleted,
                    testsTotal = groups.testsTotal,
                )
            PersonalityProfileStatus.PROCESSING,
            PersonalityProfileStatus.NOT_READY,
            -> processingPreview
        }
    }

    fun maybeTriggerGeneration(userId: UUID) {
        val groups = surveyService.listGroups(userId)
        if (groups.testsCompleted < groups.testsTotal) return

        val seeker = seekerRepository.findByUserId(userId) ?: return
        val record = profileRepository.findBySeekerId(seeker.id)

        val shouldStart =
            when {
                record == null -> true
                record.generationStatus == PersonalityProfileStatus.PROCESSING && isStale(record) -> true
                else -> false
            }
        if (!shouldStart) return

        startGeneration(seeker.id, userId)
    }

    fun triggerGeneration(userId: UUID) {
        val groups = surveyService.listGroups(userId)
        require(groups.testsCompleted >= groups.testsTotal) { "Сначала пройдите все группы тестов" }

        val seeker = seekerRepository.findByUserId(userId) ?: error("Соискатель не найден")
        val record = profileRepository.findBySeekerId(seeker.id)
        if (record?.generationStatus == PersonalityProfileStatus.PROCESSING && !isStale(record)) {
            error("Генерация уже выполняется")
        }
        startGeneration(seeker.id, userId)
    }

    private fun startGeneration(seekerId: Long, userId: UUID) {
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
        llmConfig.validateForGeneration()
        val context = surveyService.buildLlmContext(userId)
        require(context.surveys.isNotEmpty()) { "Нет завершённых опросов для интерпретации" }

        val (systemPrompt, userPrompt) = promptBuilder.build(context)
        val rawResponse = llmClient.chat(systemPrompt, userPrompt)
        val output = validator.validateAndParse(rawResponse)
        val record = PersonalityProfileMapper.fromLlmOutput(seekerId, output)
        profileRepository.upsertProfile(seekerId, record)
    }

    private fun isStale(record: jobs.procrush.models.SeekerPersonalProfileRecord): Boolean {
        // updated_at is not in record — use repository lookup is enough for active job check;
        // stale detection relies on absence of active in-memory job after server restart.
        return activeJobs[record.seekerId]?.isActive != true &&
            record.generationStatus == PersonalityProfileStatus.PROCESSING
    }
}
