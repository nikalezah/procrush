package jobs.procrush.matching.service

import jobs.procrush.matching.dto.CandidateRecommendationDto
import jobs.procrush.matching.dto.JobRecommendationDto
import java.util.UUID

interface MatchingQueries {
    fun jobRecommendationsForSeeker(userId: UUID): List<JobRecommendationDto>

    fun candidateRecommendationsForJob(
        occupationId: Long,
        jobProfileId: Long,
    ): List<CandidateRecommendationDto>

    fun jobRecommendationForSeeker(seekerId: Long, jobProfileId: Long): JobRecommendationDto?

    fun candidateRecommendationForJob(seekerId: Long, jobProfileId: Long): CandidateRecommendationDto?

    fun jobRecommendationDisplay(jobProfileId: Long): JobRecommendationDto?

    fun candidateRecommendationDisplay(seekerId: Long, jobProfileId: Long): CandidateRecommendationDto?

    fun countMatchedCandidatesForOccupation(occupationId: Long): Int

    fun countMatchedCandidatesForOccupations(occupationIds: List<Long>): Map<Long, Int>
}
