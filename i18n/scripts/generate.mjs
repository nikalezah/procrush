import {mkdirSync, readFileSync, writeFileSync} from 'node:fs'
import {dirname, join} from 'node:path'
import {fileURLToPath} from 'node:url'
import {parse as parseYaml} from 'yaml'
import {validateI18n} from './validate.mjs'

const root = join(dirname(fileURLToPath(import.meta.url)), '..')

function loadErrorCodes() {
  const parsed = parseYaml(readFileSync(join(root, 'error-codes.yaml'), 'utf8'))
  return Object.entries(parsed).map(([name, config]) => ({
    name,
    httpStatus: config.httpStatus,
    message: config.message,
  }))
}

function escapeKotlinString(value) {
  return value.replace(/\\/g, '\\\\').replace(/"/g, '\\"')
}

function generateKotlin(codes) {
  const entries = codes
    .map((c, index) => {
      const suffix = index === codes.length - 1 ? ';' : ','
      return `    ${c.name}(${c.httpStatus}, "${escapeKotlinString(c.message)}")${suffix}`
    })
    .join('\n')

  return `// Generated from i18n/error-codes.yaml — do not edit manually.
package jobs.procrush.i18n

enum class ErrorCode(
    val httpStatus: Int,
    private val messageTemplate: String,
) {
${entries}

    fun formatMessage(details: Map<String, String> = emptyMap()): String {
        var result = messageTemplate
        details.forEach { (key, value) ->
            result = result.replace("{{\$key}}", value)
        }
        return result
    }

    companion object {
        fun fromName(name: String): ErrorCode? = entries.find { it.name == name }
    }
}
`
}

function generateTypeScript(codes) {
  const union = codes.map((c) => `'${c.name}'`).join(' | ')
  const metadata = codes
    .map(
      (c) =>
        `  ${c.name}: { httpStatus: ${c.httpStatus}, message: ${JSON.stringify(c.message)} },`,
    )
    .join('\n')

  return `// Generated from i18n/error-codes.yaml — do not edit manually.

export type ErrorCode = ${union}

export const ERROR_CODES: Record<ErrorCode, { httpStatus: number; message: string }> = {
${metadata}
}

export function isErrorCode(value: string): value is ErrorCode {
  return value in ERROR_CODES
}
`
}

function generate() {
  const codes = loadErrorCodes()
  validateI18n()

  const kotlinDir = join(root, 'generated', 'kotlin', 'jobs', 'procrush', 'i18n')
  const tsDir = join(root, 'generated', 'typescript')
  mkdirSync(kotlinDir, { recursive: true })
  mkdirSync(tsDir, { recursive: true })

  writeFileSync(join(kotlinDir, 'ErrorCode.kt'), generateKotlin(codes))
  writeFileSync(join(tsDir, 'errorCodes.ts'), generateTypeScript(codes))

  console.log(`Generated ${codes.length} error codes`)
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  generate()
}
