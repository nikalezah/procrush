package jobs.procrush

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform