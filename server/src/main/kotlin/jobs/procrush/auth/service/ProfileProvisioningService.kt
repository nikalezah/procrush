package jobs.procrush.auth.service

import jobs.procrush.auth.UserRole
import jobs.procrush.employer.repository.EmployerRepository
import jobs.procrush.seeker.repository.SeekerRepository
import java.util.UUID

class ProfileProvisioningService(
    private val seekerRepository: SeekerRepository,
    private val employerRepository: EmployerRepository,
) {
    fun provisionForRole(
        userId: UUID,
        role: UserRole,
        firstName: String = "",
        lastName: String = "",
        middleName: String? = null,
        companyName: String = "",
    ) {
        when (role) {
            UserRole.SEEKER -> {
                if (seekerRepository.findByUserId(userId) == null) {
                    seekerRepository.createForUser(
                        userId = userId,
                        firstName = firstName,
                        lastName = lastName,
                        middleName = middleName,
                    )
                }
            }
            UserRole.EMPLOYER -> {
                if (employerRepository.findByUserId(userId) == null) {
                    employerRepository.createForUser(userId, name = companyName)
                }
            }
        }
    }
}
