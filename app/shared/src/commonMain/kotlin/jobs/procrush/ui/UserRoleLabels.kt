package jobs.procrush.ui

import jobs.procrush.auth.UserRole

fun UserRole.displayLabel(): String =
    when (this) {
        UserRole.SEEKER -> "Соискатель"
        UserRole.EMPLOYER -> "Работодатель"
    }
