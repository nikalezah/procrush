package jobs.procrush.auth.service

import jobs.procrush.auth.AuthUserDto
import jobs.procrush.auth.UserRole
import jobs.procrush.employer.repository.EmployerRepository
import jobs.procrush.seeker.repository.SeekerRepository
import java.util.UUID

class UserProfileEnricher(
    private val seekerRepository: SeekerRepository,
    private val employerRepository: EmployerRepository,
) {
    fun enrich(user: AuthUserDto): AuthUserDto {
        val role = user.role ?: return user.copy(profileName = null)
        val profileName =
            when (role) {
                UserRole.SEEKER ->
                    seekerRepository.findByUserId(UUID.fromString(user.id))?.let { seeker ->
                        listOf(seeker.firstName, seeker.lastName)
                            .joinToString(" ")
                            .trim()
                            .ifBlank { null }
                    }
                UserRole.EMPLOYER ->
                    employerRepository.findByUserId(UUID.fromString(user.id))?.name?.trim()?.ifBlank { null }
            }
        return user.copy(profileName = profileName)
    }
}
