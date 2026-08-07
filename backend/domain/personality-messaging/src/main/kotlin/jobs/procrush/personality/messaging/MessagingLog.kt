package jobs.procrush.personality.messaging

fun interface MessagingLog {
    fun info(
        message: String,
        vararg args: Any?,
    )
}
