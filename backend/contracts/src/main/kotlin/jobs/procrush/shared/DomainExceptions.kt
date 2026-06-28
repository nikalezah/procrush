package jobs.procrush.shared

import jobs.procrush.i18n.ErrorCode

open class CodedException(
    val errorCode: ErrorCode,
    val details: Map<String, String> = emptyMap(),
) : Exception(errorCode.formatMessage(details))

class SurveyAlreadyCompletedException(
    details: Map<String, String> = emptyMap(),
) : CodedException(ErrorCode.SURVEY_ALREADY_COMPLETED, details)

class ResourceNotFoundException(
    errorCode: ErrorCode = ErrorCode.NOT_FOUND,
    details: Map<String, String> = emptyMap(),
) : CodedException(errorCode, details)

class GenerationInProgressException(
    details: Map<String, String> = emptyMap(),
) : CodedException(ErrorCode.GENERATION_IN_PROGRESS, details)

class RegistrationConflictException(
    details: Map<String, String> = emptyMap(),
) : CodedException(ErrorCode.REGISTRATION_CONFLICT, details)

fun ErrorCode.raise(details: Map<String, String> = emptyMap()): Nothing = throw CodedException(this, details)
