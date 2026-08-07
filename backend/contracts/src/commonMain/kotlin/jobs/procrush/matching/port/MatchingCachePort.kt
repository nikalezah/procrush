package jobs.procrush.matching.port

interface MatchingCachePort {
    fun invalidateSeekerJobs(seekerId: Long)

    fun invalidateJobCandidates(jobProfileId: Long)
}
