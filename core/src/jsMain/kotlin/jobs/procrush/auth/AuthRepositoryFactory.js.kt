package jobs.procrush.auth

actual fun createAuthRepository(): AuthRepository = JsAuthRepository()
