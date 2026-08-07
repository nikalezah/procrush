package jobs.procrush.matching.dto

/** Maps stored employer name to API value; null when unset or blank. */
fun apiCompanyName(raw: String?): String? =
    raw?.trim()?.takeIf { it.isNotEmpty() && it != "—" }
