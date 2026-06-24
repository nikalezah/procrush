package jobs.procrush.matching.service

import jobs.procrush.matching.client.MatchingServiceClient
import jobs.procrush.matching.dto.CandidateRecommendationDto
import jobs.procrush.matching.dto.JobRecommendationDto
import jobs.procrush.seeker.repository.SeekerRepository
import kotlinx.coroutines.runBlocking
import java.util.UUID

class HttpMatchingQueries(
    private val client: MatchingServiceClient,
    private val seekerRepository: SeekerRepository,
) : MatchingQueries {
    override fun jobRecommendationsForSeeker(userId: UUID): List<JobRecommendationDto> {
        val seekerId = seekerRepository.findByUserId(userId)?.id ?: return emptyList()
        return runBlocking { client.jobRecommendationsForSeeker(seekerId) }
    }

    override fun candidateRecommendationsForJob(
        occupationId: Long,
        jobProfileId: Long,
    ): List<CandidateRecommendationDto> = runBlocking { client.candidateRecommendationsForJob(jobProfileId) }

    override fun jobRecommendationForSeeker(seekerId: Long, jobProfileId: Long): JobRecommendationDto? =
        runBlocking { client.jobRecommendationForSeeker(seekerId, jobProfileId) }

    override fun candidateRecommendationForJob(seekerId: Long, jobProfileId: Long): CandidateRecommendationDto? =
        runBlocking { client.candidateRecommendationForJob(seekerId, jobProfileId) }

    override fun jobRecommendationDisplay(jobProfileId: Long): JobRecommendationDto? =
        runBlocking { client.jobRecommendationDisplay(jobProfileId) }

    override fun candidateRecommendationDisplay(seekerId: Long, jobProfileId: Long): CandidateRecommendationDto? =
        runBlocking { client.candidateRecommendationDisplay(seekerId, jobProfileId) }

    override fun countMatchedCandidatesForOccupation(occupationId: Long): Int =
        runBlocking { client.countMatchedCandidatesForOccupation(occupationId) }

    override fun countMatchedCandidatesForOccupations(occupationIds: List<Long>): Map<Long, Int> =
        runBlocking { client.countMatchedCandidatesForOccupations(occupationIds) }
}
