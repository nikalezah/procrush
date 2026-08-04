package jobs.procrush.matching.service

import jobs.procrush.matching.dto.CandidateCardDto
import jobs.procrush.matching.dto.CandidateRecommendationDto
import jobs.procrush.matching.dto.JobCardDto
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
            toJobRecommendation(score.jobProfileId, score.matchScore)
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
            )
        }
    }

    override fun jobRecommendationForSeeker(seekerId: Long, jobProfileId: Long): JobRecommendationDto? {
        val score = matchScoreRepository.findPair(seekerId, jobProfileId) ?: return null
        return toJobRecommendation(jobProfileId, score.matchScore)
    }

    override fun candidateRecommendationForJob(seekerId: Long, jobProfileId: Long): CandidateRecommendationDto? {
        val job = matchingRepository.findJobProfileById(jobProfileId) ?: return null
        val score = matchScoreRepository.findPair(seekerId, jobProfileId) ?: return null
        return toCandidateRecommendation(
            seekerId = seekerId,
            positionName = job.occupationName,
            matchScore = score.matchScore,
        )
    }

    override fun jobCard(jobProfileId: Long): JobCardDto? = toJobCard(jobProfileId)

    override fun candidateCard(seekerId: Long, jobProfileId: Long): CandidateCardDto? {
        val job = matchingRepository.findJobProfileById(jobProfileId) ?: return null
        return toCandidateCard(seekerId = seekerId, positionName = job.occupationName)
    }

    override fun countMatchedCandidatesForOccupation(occupationId: Long): Int =
        matchingRepository.countEligibleSeekersForOccupations(listOf(occupationId))[occupationId] ?: 0

    override fun countMatchedCandidatesForOccupations(occupationIds: List<Long>): Map<Long, Int> =
        matchingRepository.countEligibleSeekersForOccupations(occupationIds)

    private fun toJobRecommendation(
        jobProfileId: Long,
        matchScore: Double,
    ): JobRecommendationDto? {
        val job = matchingRepository.findJobProfileById(jobProfileId) ?: return null
        return JobRecommendationDto(
            id = job.jobProfileId,
            companyName = apiCompanyName(job.companyName),
            positionName = job.occupationName,
            description = job.description.orEmpty(),
            matchScore = matchScore,
        )
    }

    private fun toJobCard(jobProfileId: Long): JobCardDto? {
        val job = matchingRepository.findJobProfileById(jobProfileId) ?: return null
        return JobCardDto(
            id = job.jobProfileId,
            companyName = apiCompanyName(job.companyName),
            positionName = job.occupationName,
            description = job.description.orEmpty(),
        )
    }

    private fun toCandidateRecommendation(
        seekerId: Long,
        positionName: String,
        matchScore: Double,
    ): CandidateRecommendationDto? {
        val seeker = seekerRepository.findById(seekerId) ?: return null
        return CandidateRecommendationDto(
            id = seeker.id,
            firstName = seeker.firstName,
            lastName = seeker.lastName,
            positionName = positionName,
            skills = resolveSkillNames(seekerId),
            matchScore = matchScore,
        )
    }

    private fun toCandidateCard(
        seekerId: Long,
        positionName: String,
    ): CandidateCardDto? {
        val seeker = seekerRepository.findById(seekerId) ?: return null
        return CandidateCardDto(
            id = seeker.id,
            firstName = seeker.firstName,
            lastName = seeker.lastName,
            positionName = positionName,
            skills = resolveSkillNames(seekerId),
        )
    }

    private fun resolveSkillNames(seekerId: Long): List<String> {
        val skillIds = seekerRepository.getSkillIds(seekerId)
        if (skillIds.isEmpty()) return emptyList()
        return referenceRepository.findSkillsByIds(skillIds).map { it.name }
    }
}
