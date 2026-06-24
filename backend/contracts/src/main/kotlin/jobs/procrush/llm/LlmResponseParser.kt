package jobs.procrush.llm

object LlmResponseParser {
    fun extractJson(raw: String): String {
        val trimmed = raw.trim()
        val fenceStart = trimmed.indexOf("```")
        if (fenceStart >= 0) {
            val afterFence = trimmed.substring(fenceStart + 3)
            val langEnd = afterFence.indexOf('\n')
            val contentStart = if (langEnd >= 0) langEnd + 1 else 0
            val fenceEnd = afterFence.indexOf("```", contentStart)
            if (fenceEnd >= 0) {
                return extractFirstJsonObject(afterFence.substring(contentStart, fenceEnd).trim())
            }
        }
        return extractFirstJsonObject(trimmed)
    }

    /** First balanced `{…}` object; ignores trailing `}` and text after valid JSON. */
    internal fun extractFirstJsonObject(text: String): String {
        val start = text.indexOf('{')
        if (start < 0) return text

        var depth = 0
        var inString = false
        var escaped = false
        for (i in start until text.length) {
            val c = text[i]
            if (inString) {
                if (escaped) {
                    escaped = false
                } else when (c) {
                    '\\' -> escaped = true
                    '"' -> inString = false
                }
            } else when (c) {
                '"' -> inString = true
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return text.substring(start, i + 1)
                }
            }
        }

        val objectEnd = text.lastIndexOf('}')
        if (objectEnd > start) return text.substring(start, objectEnd + 1)
        return text
    }
}
