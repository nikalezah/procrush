package jobs.procrush.auth.service

import jobs.procrush.auth.AuthUserDto
import jobs.procrush.auth.CompleteRegistrationRequest
import jobs.procrush.auth.UserRole
import jobs.procrush.auth.repository.UserRepository
import jobs.procrush.shared.RegistrationConflictException
import jobs.procrush.shared.ResourceNotFoundException
import java.util.UUID

class UserAuthService(
    private val userRepository: UserRepository,
    private val profileProvisioningService: ProfileProvisioningService,
    private val profileEnricher: UserProfileEnricher,
) {
    fun findDevUser(email: String): AuthUserDto? {
        val normalizedEmail = EmailNormalizer.normalize(email)
        return userRepository.findByEmail(normalizedEmail)?.let { profileEnricher.enrich(it) }
    }

    fun pendingDevRegistration(email: String): AuthUserDto {
        val normalizedEmail = EmailNormalizer.normalize(email)
        return AuthUserDto(id = "", email = normalizedEmail, role = null)
    }

    fun completeRegistration(email: String, request: CompleteRegistrationRequest): AuthUserDto {
        val normalizedEmail = EmailNormalizer.normalize(email)
        if (userRepository.findByEmail(normalizedEmail) != null) {
            throw RegistrationConflictException()
        }
        val profileData = validateRegistrationProfile(request)
        val userId =
            UUID.fromString(
                userRepository
                    .create(
                        email = normalizedEmail,
                        authProvider = DEV_PROVIDER,
                        authSubject = normalizedEmail,
                        role = request.role,
                    ).id,
            )
        profileProvisioningService.provisionForRole(
            userId = userId,
            role = request.role,
            firstName = profileData.firstName,
            lastName = profileData.lastName,
            middleName = profileData.middleName,
            companyName = profileData.companyName,
        )
        return profileEnricher.enrich(userRepository.findById(userId) ?: error("Пользователь не найден"))
    }

    fun deleteAccount(userId: UUID) {
        if (!userRepository.deleteById(userId)) {
            throw ResourceNotFoundException("Пользователь не найден")
        }
    }

    fun enrich(user: AuthUserDto): AuthUserDto = profileEnricher.enrich(user)

    private data class RegistrationProfileData(
        val firstName: String = "",
        val lastName: String = "",
        val middleName: String? = null,
        val companyName: String = "",
    )

    private fun validateRegistrationProfile(request: CompleteRegistrationRequest): RegistrationProfileData =
        when (request.role) {
            UserRole.SEEKER -> {
                val firstName = request.firstName?.trim().orEmpty()
                val lastName = request.lastName?.trim().orEmpty()
                require(firstName.isNotBlank()) { "Укажите имя" }
                require(lastName.isNotBlank()) { "Укажите фамилию" }
                RegistrationProfileData(
                    firstName = firstName,
                    lastName = lastName,
                    middleName = request.middleName?.trim()?.ifBlank { null },
                )
            }
            UserRole.EMPLOYER -> {
                val companyName = request.companyName?.trim().orEmpty()
                require(companyName.isNotBlank()) { "Укажите название компании" }
                RegistrationProfileData(companyName = companyName)
            }
        }

    private companion object {
        const val DEV_PROVIDER = "dev"
    }
}
