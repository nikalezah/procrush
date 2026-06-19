package jobs.procrush.matching.dto

object InterestStatusCalculator {
    fun forSeeker(seekerResponded: Boolean, employerResponded: Boolean): InterestStatus =
        when {
            seekerResponded && employerResponded -> InterestStatus.MUTUAL
            seekerResponded -> InterestStatus.RESPONDED
            employerResponded -> InterestStatus.INCOMING
            else -> InterestStatus.NONE
        }

    fun forEmployer(seekerResponded: Boolean, employerResponded: Boolean): InterestStatus =
        when {
            seekerResponded && employerResponded -> InterestStatus.MUTUAL
            employerResponded -> InterestStatus.RESPONDED
            seekerResponded -> InterestStatus.INCOMING
            else -> InterestStatus.NONE
        }
}
