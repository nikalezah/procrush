import {existsSync, readFileSync} from 'node:fs'
import {dirname, join} from 'node:path'
import {fileURLToPath} from 'node:url'
import {parse as parseYaml} from 'yaml'

const root = join(dirname(fileURLToPath(import.meta.url)), '..')
const locales = ['ru', 'en']

function loadJson(path) {
  if (!existsSync(path)) {
    throw new Error(`Missing file: ${path}`)
  }
  return JSON.parse(readFileSync(path, 'utf8'))
}

function loadErrorCodes() {
  const yamlPath = join(root, 'error-codes.yaml')
  const parsed = parseYaml(readFileSync(yamlPath, 'utf8'))
  return Object.keys(parsed)
}

export function validateI18n() {
  const codes = loadErrorCodes()
  const errors = []

  for (const locale of locales) {
    const errorsPath = join(root, 'locales', locale, 'errors.json')
    const translations = loadJson(errorsPath)
    const translationKeys = Object.keys(translations)

    for (const code of codes) {
      if (!(code in translations)) {
        errors.push(`Missing errors.${code} in locales/${locale}/errors.json`)
      }
    }

    for (const key of translationKeys) {
      if (!codes.includes(key)) {
        errors.push(`Unknown error code "${key}" in locales/${locale}/errors.json`)
      }
    }
  }

  if (errors.length > 0) {
    throw new Error(`i18n validation failed:\n${errors.map((e) => `  - ${e}`).join('\n')}`)
  }

  console.log(`i18n validation passed (${codes.length} error codes, ${locales.length} locales)`)
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  validateI18n()
}
