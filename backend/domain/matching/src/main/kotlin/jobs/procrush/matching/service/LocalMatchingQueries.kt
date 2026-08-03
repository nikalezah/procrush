package jobs.procrush.matching.service

import jobs.procrush.matching.dto.CandidateRecommendationDto
import jobs.procrush.matching.dto.JobRecommendationDto
import jobs.procrush.matching.dto.apiCompanyName
import jobs.procrush.matching.repository.MatchScoreRepository
import jobs.procrush.matching.repository.MatchingRepository
import jobs.procrush.seeker.repository.SeekerRepository
import jobs.procrush.shared.repository.ReferenceRepository
import java.util.UUID

class LocalMatchingQueries(
    private val matchScoreRepository: MatchScoreRepository,
    private val matchingRepository: MatchingRepository,
    private val seekerRepository: SeekerRepository,
    private val referenceRepository: ReferenceRepository,
) : MatchingQueries {
    override fun jobRecommendationsForSeeker(userId: UUID): List<JobRecommendationDto> {
        val seekerId = seekerRepository.findByUserId(userId)?.id ?: return emptyList()
        return matchScoreRepository.listForSeeker(seekerId).mapNotNull { score ->
            toJobRecommendation(score.jobProfileId, score.matchScore, score.matchScoreDisplay)
        }
    }

    override fun candidateRecommendationsForJob(
        occupationId: Long,
        jobProfileId: Long,
    ): List<CandidateRecommendationDto> {
        val job = matchingRepository.findJobProfileById(jobProfileId) ?: return emptyList()
        return matchScoreRepository.listForJob(jobProfileId).mapNotNull { score ->
            toCandidateRecommendation(
                seekerId = score.seekerId,
                positionName = job.occupationName,
                matchScore = score.matchScore,
                matchScoreDisplay = score.matchScoreDisplay,
            )
        }
    }

    override fun jobRecommendationForSeeker(seekerId: Long, jobProfileId: Long): JobRecommendationDto? {
        val score = matchScoreRepository.findPair(seekerId, jobProfileId) ?: return null
        return toJobRecommendation(jobProfileId, score.matchScore, score.matchScoreDisplay)
    }

    override fun candidateRecommendationForJob(seekerId: Long, jobProfileId: Long): CandidateRecommendationDto? {
        val job = matchingRepository.findJobProfileById(jobProfileId) ?: return null
        val score = matchScoreRepository.findPair(seekerId, jobProfileId) ?: return null
        return toCandidateRecommendation(
            seekerId = seekerId,
            positionName = job.occupationName,
            matchScore = score.matchScore,
            matchScoreDisplay = score.matchScoreDisplay,
        )
    }

    override fun jobRecommendationDisplay(jobProfileId: Long): JobRecommendationDto? =
        toJobRecommendation(jobProfileId, matchScore = 0.0, matchScoreDisplay = 0)

    override fun candidateRecommendationDisplay(seekerId: Long, jobProfileId: Long): CandidateRecommendationDto? {
        val job = matchingRepository.findJobProfileById(jobProfileId) ?: return null
        return toCandidateRecommendation(
            seekerId = seekerId,
            positionName = job.occupationName,
            matchScore = 0.0,
            matchScoreDisplay = 0,
        )
    }

    override fun countMatchedCandidatesForOccupation(occupationId: Long): Int =
        matchingRepository.countEligibleSeekersForOccupations(listOf(occupationId))[occupationId] ?: 0

    override fun countMatchedCandidatesForOccupations(occupationIds: List<Long>): Map<Long, Int> =
        matchingRepository.countEligibleSeekersForOccupations(occupationIds)

    private fun toJobRecommendation(
        jobProfileId: Long,
        matchScore: Double,
        matchScoreDisplay: Int,
    ): JobRecommendationDto? {
        val job = matchingRepository.findJobProfileById(jobProfileId) ?: return null
        if (!job.isActive && matchScoreDisplay == 0 && matchScore == 0.0) {
            // display fallback still allowed for inactive jobs (interests outside)
        }
        return JobRecommendationDto(
            id = job.jobProfileId,
            companyName = apiCompanyName(job.companyName),
            positionName = job.occupationName,
            description = job.description.orEmpty(),
            matchScore = matchScore,
            matchScoreDisplay = matchScoreDisplay,
        )
    }

    private fun toCandidateRecommendation(
        seekerId: Long,
        positionName: String,
        matchScore: Double,
        matchScoreDisplay: Int,
    ): CandidateRecommendationDto? {
        val seeker = seekerRepository.findById(seekerId) ?: return null
        val skillIds = seekerRepository.getSkillIds(seekerId)
        val skills =
            if (skillIds.isEmpty()) {
                emptyList()
            } else {
                referenceRepository.findSkillsByIds(skillIds).map { it.name }
            }
        return CandidateRecommendationDto(
            id = seeker.id,
            firstName = seeker.firstName,
            lastName = seeker.lastName,
            positionName = positionName,
            skills = skills,
            matchScore = matchScore,
            matchScoreDisplay = matchScoreDisplay,
        )
    }
}
