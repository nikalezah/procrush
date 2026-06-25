package jobs.procrush.composition

import jobs.procrush.employer.repository.EmployerRepository
import jobs.procrush.seeker.repository.SeekerRepository
import jobs.procrush.shared.repository.ReferenceRepository

data class AuthModule(
    val seekerRepository: SeekerRepository,
    val referenceRepository: ReferenceRepository,
    val employerRepository: EmployerRepository,
) {
    companion object {
        fun create(): AuthModule {
            val referenceRepository = ReferenceRepository()
            val seekerRepository = SeekerRepository()
            val employerRepository = EmployerRepository(referenceRepository)
            return AuthModule(
                seekerRepository = seekerRepository,
                referenceRepository = referenceRepository,
                employerRepository = employerRepository,
            )
        }
    }
}
