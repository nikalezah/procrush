package jobs.procrush.personality.service

import io.ktor.client.plugins.HttpRequestTimeoutException
import jobs.procrush.bootstrap.config.LlmConfig
import jobs.procrush.i18n.ErrorCode
import jobs.procrush.llm.LlmClient
import jobs.procrush.personality.llm.PersonalityProfileValidator
import jobs.procrush.personality.llm.PersonalityPromptBuilder
import jobs.procrush.personality.messaging.PersonalityGenerationCommand
import jobs.procrush.personality.messaging.PersonalityGenerationResult
import jobs.procrush.personality.messaging.PersonalityGenerationResultStatus
import jobs.procrush.personality.observability.Logger
import jobs.procrush.personality.observability.Metrics
import jobs.procrush.shared.raise

class PersonalityGenerationHandler(
    private val llmConfig: LlmConfig,
    private val llmClient: LlmClient,
    private val promptBuilder: PersonalityPromptBuilder,
    private val validator: PersonalityProfileValidator,
) {
    private val logger = Logger.get(PersonalityGenerationHandler::class.java)

    suspend fun generate(command: PersonalityGenerationCommand): PersonalityGenerationResult {
        llmConfig.validateForGeneration()
        if (command.surveyContext.surveys.isEmpty()) {
            ErrorCode.NO_COMPLETED_SURVEYS.raise()
        }

        val catalogNames = command.catalog.map { it.name }.toSet()
        val (systemPrompt, userPrompt) = promptBuilder.build(command.surveyContext, command.catalog)
        val rawResponse =
            Metrics.recordPersonalityLlm {
                llmClient.chat(systemPrompt, userPrompt)
            }
        val output = validator.validateAndParse(rawResponse, catalogNames)
        logger.info(
            "Personality LLM generation succeeded seekerId={} attempt={}",
            command.seekerId,
            command.attempt,
        )
        return PersonalityGenerationResult(
            seekerId = command.seekerId,
            userId = command.userId,
            status = PersonalityGenerationResultStatus.SUCCESS,
            attempt = command.attempt,
            correlationId = command.correlationId,
            profile = output,
        )
    }

    fun failureResult(
        command: PersonalityGenerationCommand,
        error: Throwable,
        commandMessageId: String?,
    ): PersonalityGenerationResult =
        PersonalityGenerationResult(
            seekerId = command.seekerId,
            userId = command.userId,
            status = PersonalityGenerationResultStatus.FAILED,
            attempt = command.attempt,
            correlationId = command.correlationId,
            commandMessageId = commandMessageId,
            errorCode = failureCode(error),
        )

    fun failureCode(error: Throwable): String =
        when (error) {
            is HttpRequestTimeoutException -> ErrorCode.LLM_TIMEOUT.name
            is jobs.procrush.shared.CodedException -> error.errorCode.name
            else -> ErrorCode.UNKNOWN_ERROR.name
        }
}
