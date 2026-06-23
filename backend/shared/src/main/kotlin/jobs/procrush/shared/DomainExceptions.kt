package jobs.procrush.shared

class SurveyAlreadyCompletedException(message: String = "Опрос уже пройден") : IllegalStateException(message)

class ResourceNotFoundException(message: String = "Не найдено") : IllegalStateException(message)

class GenerationInProgressException(message: String = "Генерация уже выполняется") : IllegalStateException(message)

class RegistrationConflictException(message: String = "Пользователь уже зарегистрирован") : IllegalStateException(message)
