package jobs.procrush.auth

import kotlinx.browser.window

actual fun currentAuthPath(): String = window.location.pathname
