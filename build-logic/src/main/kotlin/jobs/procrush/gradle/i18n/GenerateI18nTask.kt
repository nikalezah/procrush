package jobs.procrush.gradle.i18n

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.yaml.snakeyaml.Yaml
import java.io.File

@CacheableTask
abstract class GenerateI18nTask : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val errorCodesYaml: RegularFileProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val localesDir: DirectoryProperty

    @get:OutputFile
    abstract val generatedKotlin: RegularFileProperty

    @get:OutputFile
    abstract val generatedTypescript: RegularFileProperty

    init {
        group = "i18n"
        description = "Validate locales and generate ErrorCode.kt / errorCodes.ts from i18n/error-codes.yaml"
    }

    @TaskAction
    fun generate() {
        val codes = loadErrorCodes(errorCodesYaml.get().asFile)
        validateLocales(codes.map { it.name }.toSet(), localesDir.get().asFile)

        val kotlinFile = generatedKotlin.get().asFile
        kotlinFile.parentFile.mkdirs()
        kotlinFile.writeText(generateKotlin(codes))

        val typescriptFile = generatedTypescript.get().asFile
        typescriptFile.parentFile.mkdirs()
        typescriptFile.writeText(generateTypeScript(codes))

        logger.lifecycle("Generated ${codes.size} error codes")
    }

    private data class ErrorCodeDef(
        val name: String,
        val httpStatus: Int,
        val message: String,
    )

    private fun loadErrorCodes(yamlFile: File): List<ErrorCodeDef> {
        @Suppress("UNCHECKED_CAST")
        val parsed = Yaml().load<Map<String, Any?>>(yamlFile.readText())
            ?: throw GradleException("Empty or invalid ${yamlFile.path}")

        return parsed.map { (name, raw) ->
            val config = raw as? Map<*, *>
                ?: throw GradleException("Invalid config for error code $name in ${yamlFile.path}")
            val httpStatus = when (val status = config["httpStatus"]) {
                is Number -> status.toInt()
                else -> throw GradleException("Missing or invalid httpStatus for $name")
            }
            val message = config["message"] as? String
                ?: throw GradleException("Missing or invalid message for $name")
            ErrorCodeDef(name = name, httpStatus = httpStatus, message = message)
        }
    }

    private fun validateLocales(codes: Set<String>, localesRoot: File) {
        val errors = mutableListOf<String>()
        for (locale in listOf("ru", "en")) {
            val errorsFile = File(localesRoot, "$locale/errors.json")
            if (!errorsFile.isFile) {
                errors += "Missing file: ${errorsFile.path}"
                continue
            }
            @Suppress("UNCHECKED_CAST")
            val translations = Yaml().load<Map<String, Any?>>(errorsFile.readText())?.keys.orEmpty()
            for (code in codes) {
                if (code !in translations) {
                    errors += "Missing errors.$code in locales/$locale/errors.json"
                }
            }
            for (key in translations) {
                if (key !in codes) {
                    errors += "Unknown error code \"$key\" in locales/$locale/errors.json"
                }
            }
        }
        if (errors.isNotEmpty()) {
            throw GradleException(
                "i18n validation failed:\n${errors.joinToString("\n") { "  - $it" }}",
            )
        }
        logger.lifecycle("i18n validation passed (${codes.size} error codes, 2 locales)")
    }

    private fun escapeKotlinString(value: String): String =
        value.replace("\\", "\\\\").replace("\"", "\\\"")

    private fun escapeJsonString(value: String): String = buildString {
        append('"')
        for (ch in value) {
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(ch)
            }
        }
        append('"')
    }

    private fun generateKotlin(codes: List<ErrorCodeDef>): String {
        val entries = codes.mapIndexed { index, code ->
            val suffix = if (index == codes.lastIndex) ";" else ","
            "    ${code.name}(${code.httpStatus}, \"${escapeKotlinString(code.message)}\")$suffix"
        }.joinToString("\n")

        return """
            |// Generated from i18n/error-codes.yaml -- do not edit manually.
            |package jobs.procrush.i18n
            |
            |enum class ErrorCode(
            |    val httpStatus: Int,
            |    private val messageTemplate: String,
            |) {
            |$entries
            |
            |    fun formatMessage(details: Map<String, String> = emptyMap()): String {
            |        var result = messageTemplate
            |        details.forEach { (key, value) ->
            |            result = result.replace("{{${'$'}key}}", value)
            |        }
            |        return result
            |    }
            |
            |    companion object {
            |        fun fromName(name: String): ErrorCode? = entries.find { it.name == name }
            |    }
            |}
            |
        """.trimMargin()
    }

    private fun generateTypeScript(codes: List<ErrorCodeDef>): String {
        val union = codes.joinToString(" | ") { "'${it.name}'" }
        val metadata = codes.joinToString("\n") { code ->
            "  ${code.name}: { httpStatus: ${code.httpStatus}, message: ${escapeJsonString(code.message)} },"
        }

        return """
            |// Generated from i18n/error-codes.yaml -- do not edit manually.
            |
            |export type ErrorCode = $union
            |
            |export const ERROR_CODES: Record<ErrorCode, { httpStatus: number; message: string }> = {
            |$metadata
            |}
            |
            |export function isErrorCode(value: string): value is ErrorCode {
            |  return value in ERROR_CODES
            |}
            |
        """.trimMargin()
    }
}
