package jobs.procrush.matching.service

import jobs.procrush.matching.dto.CandidateRecommendationDto
import jobs.procrush.matching.dto.JobRecommendationDto
import jobs.procrush.matching.model.JobMatchCandidate
import jobs.procrush.matching.model.SeekerMatchCandidate
import jobs.procrush.matching.model.SeekerMatchingContext
import jobs.procrush.matching.repository.MatchingRepository
import jobs.procrush.seeker.repository.SeekerRepository
import jobs.procrush.shared.ResourceNotFoundException
import jobs.procrush.survey.service.SurveyService
import java.util.UUID

class MatchingService(
    private val seekerRepository: SeekerRepository,
    private val matchingRepository: MatchingRepository,
    private val surveyService: SurveyService,
) {
    fun jobRecommendationsForSeeker(userId: UUID): List<JobRecommendationDto> {
        val seeker = seekerRepository.findByUserId(userId) ?: return emptyList()
        val surveyGroups = surveyService.listGroups(userId)
        if (surveyGroups.testsCompleted < surveyGroups.testsTotal) return emptyList()

        val occupationIds = seekerRepository.getDesiredOccupationIds(seeker.id)
        if (occupationIds.isEmpty()) return emptyList()

        val seekerContext =
            matchingRepository.getSeekerMatchingContext(seeker.id, testsAlreadyComplete = true)
                ?: return emptyList()

        return matchingRepository
            .findMatchableJobProfiles(occupationIds)
            .map { job -> scoreJob(job, seekerContext) }
            .sortedByDescending { it.matchScore }
    }

    fun candidateRecommendationsForJob(
        occupationId: Long,
        jobProfileId: Long,
    ): List<CandidateRecommendationDto> {
        val jobProfile =
            matchingRepository.findMatchableJobProfiles(listOf(occupationId))
                .firstOrNull { it.jobProfileId == jobProfileId }
                ?: throw ResourceNotFoundException("Профиль не найден")

        return matchingRepository
            .findMatchableSeekers(occupationId)
            .map { seeker -> scoreCandidate(seeker, jobProfile) }
            .sortedByDescending { it.matchScore }
    }

    fun jobRecommendationForSeeker(seekerId: Long, jobProfileId: Long): JobRecommendationDto? {
        val job = matchingRepository.findJobProfileById(jobProfileId) ?: return null
        val seekerContext = matchingRepository.getSeekerMatchingContext(seekerId) ?: return null
        return scoreJob(job, seekerContext)
    }

    fun candidateRecommendationForJob(seekerId: Long, jobProfileId: Long): CandidateRecommendationDto? {
        val job = matchingRepository.findJobProfileById(jobProfileId) ?: return null
        val seeker =
            matchingRepository.findSeekerMatchCandidate(seekerId, job.occupationId) ?: return null
        return scoreCandidate(seeker, job)
    }

    fun jobRecommendationFallback(jobProfileId: Long): JobRecommendationDto? {
        val job = matchingRepository.findJobProfileById(jobProfileId) ?: return null
        return JobRecommendationDto(
            id = job.jobProfileId,
            companyName = job.companyName,
            positionName = job.occupationName,
            description = job.description.orEmpty(),
            matchScore = 0.0,
            matchScoreDisplay = 0,
        )
    }

    fun candidateRecommendationFallback(seekerId: Long, jobProfileId: Long): CandidateRecommendationDto? {
        val job = matchingRepository.findJobProfileById(jobProfileId) ?: return null
        val seeker =
            matchingRepository.findSeekerMatchCandidate(seekerId, job.occupationId) ?: return null
        return CandidateRecommendationDto(
            id = seeker.seekerId,
            firstName = seeker.firstName,
            lastName = seeker.lastName,
            positionName = seeker.occupationName,
            skills = seeker.skillNames,
            matchScore = 0.0,
            matchScoreDisplay = 0,
        )
    }

    fun countMatchedCandidatesForOccupation(occupationId: Long): Int =
        matchingRepository.countMatchableSeekers(occupationId)

    fun countMatchedCandidatesForOccupations(occupationIds: List<Long>): Map<Long, Int> =
        matchingRepository.countMatchableSeekersByOccupations(occupationIds)

    private fun scoreJob(
        job: JobMatchCandidate,
        seeker: SeekerMatchingContext,
    ): JobRecommendationDto {
        val skills = MatchScoringService.skillsScore(seeker.skillIds, job.skillIds)
        val personality =
            if (seeker.personalityReady && seeker.personalityAxes != null) {
                MatchScoringService.personalityScore(seeker.personalityAxes, job.personalityAxes)
            } else {
                null
            }
        val matchScore =
            MatchScoringService.combinedScore(skills, personality, seeker.personalityReady)

        return JobRecommendationDto(
            id = job.jobProfileId,
            companyName = job.companyName,
            positionName = job.occupationName,
            description = job.description.orEmpty(),
            matchScore = matchScore,
            matchScoreDisplay = MatchScoringService.toDisplayScore(matchScore),
        )
    }

    private fun scoreCandidate(
        seeker: SeekerMatchCandidate,
        job: JobMatchCandidate,
    ): CandidateRecommendationDto {
        val skills = MatchScoringService.skillsScore(seeker.skillIds, job.skillIds)
        val personality =
            if (seeker.personalityReady && seeker.personalityAxes != null) {
                MatchScoringService.personalityScore(seeker.personalityAxes, job.personalityAxes)
            } else {
                null
            }
        val matchScore =
            MatchScoringService.combinedScore(skills, personality, seeker.personalityReady)

        return CandidateRecommendationDto(
            id = seeker.seekerId,
            firstName = seeker.firstName,
            lastName = seeker.lastName,
            positionName = seeker.occupationName,
            skills = seeker.skillNames,
            matchScore = matchScore,
            matchScoreDisplay = MatchScoringService.toDisplayScore(matchScore),
        )
    }
}
