package jobs.procrush.fixtures

import jobs.procrush.models.CandidateRecommendationDto
import jobs.procrush.models.JobRecommendationDto

object RecommendationStubs {
    fun jobRecommendations(): List<JobRecommendationDto> =
        listOf(
            JobRecommendationDto(
                id = 1,
                companyName = "TechNova",
                positionName = "Backend-разработчик",
                description = "Разработка микросервисов на Kotlin, участие в архитектурных решениях.",
                matchScore = 0.82,
                matchScoreDisplay = 4,
                testsCompleted = 1,
                isScoreReduced = true,
            ),
            JobRecommendationDto(
                id = 2,
                companyName = "DataFlow",
                positionName = "Fullstack-разработчик",
                description = "React + Ktor, продуктовая команда, гибкий график.",
                matchScore = 0.76,
                matchScoreDisplay = 4,
                testsCompleted = 1,
                isScoreReduced = true,
            ),
            JobRecommendationDto(
                id = 3,
                companyName = "CloudScale",
                positionName = "DevOps-инженер",
                description = "Kubernetes, CI/CD, облачная инфраструктура.",
                matchScore = 0.68,
                matchScoreDisplay = 3,
                testsCompleted = 1,
                isScoreReduced = true,
            ),
            JobRecommendationDto(
                id = 4,
                companyName = "ProductLab",
                positionName = "Product Manager",
                description = "Управление продуктом в IT-команде, работа с метриками.",
                matchScore = 0.61,
                matchScoreDisplay = 3,
                testsCompleted = 1,
                isScoreReduced = true,
            ),
        )

    fun candidateRecommendations(@Suppress("UNUSED_PARAMETER") jobProfileId: Long): List<CandidateRecommendationDto> =
        listOf(
            CandidateRecommendationDto(
                id = 101,
                firstName = "Алексей",
                lastName = "Петров",
                positionName = "Backend-разработчик",
                skills = listOf("Kotlin", "PostgreSQL", "Docker"),
                matchScore = 0.88,
                matchScoreDisplay = 4,
                testsCompleted = 3,
                isScoreReduced = false,
            ),
            CandidateRecommendationDto(
                id = 102,
                firstName = "Мария",
                lastName = "Сидорова",
                positionName = "Backend-разработчик",
                skills = listOf("Java", "Spring Boot", "Redis"),
                matchScore = 0.79,
                matchScoreDisplay = 4,
                testsCompleted = 2,
                isScoreReduced = true,
            ),
            CandidateRecommendationDto(
                id = 103,
                firstName = "Дмитрий",
                lastName = "Козлов",
                positionName = "Backend-разработчик",
                skills = listOf("Kotlin", "Ktor", "AWS"),
                matchScore = 0.72,
                matchScoreDisplay = 4,
                testsCompleted = 1,
                isScoreReduced = true,
            ),
        )
}
