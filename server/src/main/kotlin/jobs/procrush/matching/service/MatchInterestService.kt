package jobs.procrush.matching.service

import jobs.procrush.employer.repository.EmployerRepository
import jobs.procrush.matching.dto.CandidateRecommendationDto
import jobs.procrush.matching.dto.EmployerContactDto
import jobs.procrush.matching.dto.EmployerInterestsResponseDto
import jobs.procrush.matching.dto.InterestStatus
import jobs.procrush.matching.dto.InterestStatusCalculator
import jobs.procrush.matching.dto.JobRecommendationDto
import jobs.procrush.matching.dto.MatchInterestCountDto
import jobs.procrush.matching.dto.MatchInterestEventDto
import jobs.procrush.matching.dto.SeekerInterestsResponseDto
import jobs.procrush.matching.model.MatchInterestRecord
import jobs.procrush.matching.repository.MatchInterestRepository
import jobs.procrush.matching.repository.MatchingRepository
import jobs.procrush.seeker.repository.SeekerRepository
import jobs.procrush.shared.ResourceNotFoundException
import jobs.procrush.survey.service.SurveyService
import java.util.UUID

class MatchInterestService(
    private val seekerRepository: SeekerRepository,
    private val employerRepository: EmployerRepository,
    private val matchingService: MatchingService,
    private val matchingRepository: MatchingRepository,
    private val matchInterestRepository: MatchInterestRepository,
    private val surveyService: SurveyService,
    private val notifier: MatchInterestNotifier,
) {
    fun seekerRespond(userId: UUID, jobProfileId: Long): JobRecommendationDto {
        val seekerId = requireSeekerEligibleForJob(userId, jobProfileId)
        val (interest, isNewResponse) = matchInterestRepository.recordSeekerResponse(seekerId, jobProfileId)
        if (isNewResponse) {
            notifyEmployerOfSeekerResponse(seekerId, jobProfileId, interest)
        }
        val recommendation =
            matchingService.jobRecommendationForSeeker(seekerId, jobProfileId)
                ?: matchingService.jobRecommendationFallback(jobProfileId)
                ?: throw ResourceNotFoundException("Вакансия не найдена")
        return enrichJobRecommendation(seekerId, recommendation, interest)
    }

    fun employerRespond(userId: UUID, jobProfileId: Long, seekerId: Long): CandidateRecommendationDto {
        requireEmployerOwnsJobProfile(userId, jobProfileId)
        requireSeekerEligibleForOccupation(seekerId, jobProfileId)
        val (interest, isNewResponse) = matchInterestRepository.recordEmployerResponse(seekerId, jobProfileId)
        if (isNewResponse) {
            notifySeekerOfEmployerResponse(seekerId, jobProfileId, interest)
        }
        val recommendation =
            matchingService.candidateRecommendationForJob(seekerId, jobProfileId)
                ?: matchingService.candidateRecommendationFallback(seekerId, jobProfileId)
                ?: throw ResourceNotFoundException("Кандидат не найден")
        return enrichCandidateRecommendation(jobProfileId, recommendation, interest)
    }

    fun actionableCountForSeeker(userId: UUID): MatchInterestCountDto {
        val seeker =
            seekerRepository.findByUserId(userId)
                ?: return MatchInterestCountDto(0)
        return MatchInterestCountDto(matchInterestRepository.countActionableForSeeker(seeker.id))
    }

    fun actionableCountForEmployer(userId: UUID): MatchInterestCountDto {
        val employer =
            employerRepository.findByUserId(userId)
                ?: return MatchInterestCountDto(0)
        return MatchInterestCountDto(matchInterestRepository.countActionableForEmployer(employer.id))
    }

    suspend fun streamEvents(
        userId: UUID,
        onEvent: suspend (MatchInterestEventDto) -> Unit,
    ) {
        val channel = notifier.subscribe(userId)
        try {
            for (event in channel) {
                onEvent(event)
            }
        } finally {
            notifier.unsubscribe(userId, channel)
        }
    }

    fun enrichJobRecommendations(
        seekerId: Long,
        recommendations: List<JobRecommendationDto>,
    ): List<JobRecommendationDto> {
        if (recommendations.isEmpty()) return recommendations
        val interests =
            matchInterestRepository.findBySeekerAndJobProfiles(
                seekerId,
                recommendations.map { it.id },
            )
        return recommendations.map { rec ->
            enrichJobRecommendation(seekerId, rec, interests[rec.id])
        }
    }

    fun enrichCandidateRecommendations(
        jobProfileId: Long,
        recommendations: List<CandidateRecommendationDto>,
    ): List<CandidateRecommendationDto> {
        if (recommendations.isEmpty()) return recommendations
        val interests =
            matchInterestRepository.findByJobProfileAndSeekers(
                jobProfileId,
                recommendations.map { it.id },
            )
        return recommendations.map { rec ->
            enrichCandidateRecommendation(jobProfileId, rec, interests[rec.id])
        }
    }

    fun seekerInterestsOutsideRecommendations(
        userId: UUID,
        currentRecommendationIds: Set<Long>,
    ): SeekerInterestsResponseDto {
        val seeker =
            seekerRepository.findByUserId(userId)
                ?: return SeekerInterestsResponseDto(emptyList(), emptyList())
        val outside =
            matchInterestRepository
                .listBySeeker(seeker.id)
                .filter { it.jobProfileId !in currentRecommendationIds }

        val respondedOutside = mutableListOf<JobRecommendationDto>()
        val mutualOutside = mutableListOf<JobRecommendationDto>()

        for (interest in outside) {
            val base =
                matchingService.jobRecommendationForSeeker(seeker.id, interest.jobProfileId)
                    ?: matchingService.jobRecommendationFallback(interest.jobProfileId)
                    ?: continue
            val enriched = enrichJobRecommendation(seeker.id, base, interest)
            if (enriched.interestStatus == InterestStatus.MUTUAL) {
                mutualOutside.add(enriched)
            } else if (enriched.interestStatus == InterestStatus.RESPONDED ||
                enriched.interestStatus == InterestStatus.INCOMING
            ) {
                respondedOutside.add(enriched)
            }
        }

        return SeekerInterestsResponseDto(
            respondedOutside = respondedOutside,
            mutualOutside = mutualOutside,
        )
    }

    fun employerInterestsOutsideRecommendations(
        userId: UUID,
        jobProfileId: Long,
        currentRecommendationIds: Set<Long>,
    ): EmployerInterestsResponseDto {
        val employer =
            employerRepository.findByUserId(userId)
                ?: return EmployerInterestsResponseDto(emptyList(), emptyList())
        if (employerRepository.findJobProfile(employer.id, jobProfileId) == null) {
            throw ResourceNotFoundException("Профиль не найден")
        }

        val outside =
            matchInterestRepository
                .listByJobProfile(jobProfileId)
                .filter { it.seekerId !in currentRecommendationIds }

        val respondedOutside = mutableListOf<CandidateRecommendationDto>()
        val mutualOutside = mutableListOf<CandidateRecommendationDto>()

        for (interest in outside) {
            val base =
                matchingService.candidateRecommendationForJob(interest.seekerId, jobProfileId)
                    ?: matchingService.candidateRecommendationFallback(interest.seekerId, jobProfileId)
                    ?: continue
            val enriched = enrichCandidateRecommendation(jobProfileId, base, interest)
            if (enriched.interestStatus == InterestStatus.MUTUAL) {
                mutualOutside.add(enriched)
            } else if (enriched.interestStatus == InterestStatus.RESPONDED ||
                enriched.interestStatus == InterestStatus.INCOMING
            ) {
                respondedOutside.add(enriched)
            }
        }

        return EmployerInterestsResponseDto(
            respondedOutside = respondedOutside,
            mutualOutside = mutualOutside,
        )
    }

    private fun notifyEmployerOfSeekerResponse(
        seekerId: Long,
        jobProfileId: Long,
        interest: MatchInterestRecord,
    ) {
        val job = matchingRepository.findJobProfileById(jobProfileId) ?: return
        val employerUserId = employerRepository.findUserIdByEmployerId(job.employerId) ?: return
        val status =
            InterestStatusCalculator.forEmployer(
                interest.seekerResponded,
                interest.employerResponded,
            )
        if (status != InterestStatus.INCOMING && status != InterestStatus.MUTUAL) return

        val base =
            matchingService.candidateRecommendationForJob(seekerId, jobProfileId)
                ?: matchingService.candidateRecommendationFallback(seekerId, jobProfileId)
                ?: return
        val enriched = enrichCandidateRecommendation(jobProfileId, base, interest)
        notifier.notify(
            employerUserId,
            MatchInterestEventDto(
                jobProfileId = jobProfileId,
                seekerId = seekerId,
                interestStatus = enriched.interestStatus,
                seekerContact = enriched.contactInfo,
            ),
        )
    }

    private fun notifySeekerOfEmployerResponse(
        seekerId: Long,
        jobProfileId: Long,
        interest: MatchInterestRecord,
    ) {
        val seekerUserId = seekerRepository.findUserIdBySeekerId(seekerId) ?: return
        val status =
            InterestStatusCalculator.forSeeker(
                interest.seekerResponded,
                interest.employerResponded,
            )
        if (status != InterestStatus.INCOMING && status != InterestStatus.MUTUAL) return

        val base =
            matchingService.jobRecommendationForSeeker(seekerId, jobProfileId)
                ?: matchingService.jobRecommendationFallback(jobProfileId)
                ?: return
        val enriched = enrichJobRecommendation(seekerId, base, interest)
        notifier.notify(
            seekerUserId,
            MatchInterestEventDto(
                jobProfileId = jobProfileId,
                seekerId = seekerId,
                interestStatus = enriched.interestStatus,
                employerContact = enriched.contactInfo,
            ),
        )
    }

    private fun enrichJobRecommendation(
        seekerId: Long,
        recommendation: JobRecommendationDto,
        interest: MatchInterestRecord? = null,
    ): JobRecommendationDto {
        val resolvedInterest =
            interest
                ?: matchInterestRepository
                    .findBySeekerAndJobProfiles(seekerId, listOf(recommendation.id))
                    .values
                    .firstOrNull()
        val status =
            if (resolvedInterest == null) {
                InterestStatus.NONE
            } else {
                InterestStatusCalculator.forSeeker(
                    resolvedInterest.seekerResponded,
                    resolvedInterest.employerResponded,
                )
            }
        val contactInfo =
            if (status == InterestStatus.MUTUAL) {
                loadEmployerContactForJob(recommendation.id)
            } else {
                null
            }
        return recommendation.copy(interestStatus = status, contactInfo = contactInfo)
    }

    private fun enrichCandidateRecommendation(
        jobProfileId: Long,
        recommendation: CandidateRecommendationDto,
        interest: MatchInterestRecord? = null,
    ): CandidateRecommendationDto {
        val resolvedInterest =
            interest
                ?: matchInterestRepository
                    .findByJobProfileAndSeekers(jobProfileId, listOf(recommendation.id))
                    .values
                    .firstOrNull()
        val status =
            if (resolvedInterest == null) {
                InterestStatus.NONE
            } else {
                InterestStatusCalculator.forEmployer(
                    resolvedInterest.seekerResponded,
                    resolvedInterest.employerResponded,
                )
            }
        val contactInfo =
            if (status == InterestStatus.MUTUAL) {
                matchInterestRepository.findSeekerContact(recommendation.id)
            } else {
                null
            }
        return recommendation.copy(interestStatus = status, contactInfo = contactInfo)
    }

    private fun loadEmployerContactForJob(jobProfileId: Long): EmployerContactDto? {
        val job = matchingRepository.findJobProfileById(jobProfileId) ?: return null
        return matchInterestRepository.findEmployerContact(job.employerId)
    }

    private fun requireSeekerEligibleForJob(userId: UUID, jobProfileId: Long): Long {
        val seeker =
            seekerRepository.findByUserId(userId)
                ?: throw ResourceNotFoundException("Профиль соискателя не найден")
        val surveyGroups = surveyService.listGroups(userId)
        require(surveyGroups.testsCompleted >= surveyGroups.testsTotal) {
            "Пройдите оба теста личности для участия в подборе"
        }

        val job =
            matchingRepository.findJobProfileById(jobProfileId)
                ?: throw ResourceNotFoundException("Вакансия не найдена")
        require(job.isActive) { "Вакансия неактивна" }

        val occupationIds = seekerRepository.getDesiredOccupationIds(seeker.id)
        require(job.occupationId in occupationIds) {
            "Укажите эту должность в списке желаемых"
        }

        matchingRepository.getSeekerMatchingContext(seeker.id)
            ?: throw IllegalArgumentException("Профиль не готов для подбора")

        return seeker.id
    }

    private fun requireEmployerOwnsJobProfile(userId: UUID, jobProfileId: Long): Long {
        val employer =
            employerRepository.findByUserId(userId)
                ?: throw ResourceNotFoundException("Профиль компании не найден")
        employerRepository.findJobProfile(employer.id, jobProfileId)
            ?: throw ResourceNotFoundException("Профиль не найден")
        return employer.id
    }

    private fun requireSeekerEligibleForOccupation(seekerId: Long, jobProfileId: Long) {
        val job =
            matchingRepository.findJobProfileById(jobProfileId)
                ?: throw ResourceNotFoundException("Профиль не найден")
        val eligibleSeekerIds = matchingRepository.findSeekerIdsWithAllTestsComplete()
        require(seekerId in eligibleSeekerIds) {
            "Кандидат не прошёл обязательные тесты"
        }
        val desiredOccupations = seekerRepository.getDesiredOccupationIds(seekerId)
        require(job.occupationId in desiredOccupations) {
            "Кандидат не указал эту должность"
        }
    }
}
