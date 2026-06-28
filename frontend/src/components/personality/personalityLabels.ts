import type {TFunction} from 'i18next'
import type {PersonalityAxesDto} from '../../api/types'
import i18n from '../../i18n/config'

export const CATEGORY_ORDER = ['connections', 'creativity', 'drive', 'thinking'] as const

export const AXIS_KEYS = [
  'axisDominance',
  'axisInfluence',
  'axisStability',
  'axisIntegrity',
  'axisAutonomy',
  'axisPace',
] as (keyof PersonalityAxesDto)[]

function translate(t: TFunction | undefined, key: string): string {
  return t != null ? t(key) : i18n.t(key)
}

export function categoryLabel(key: string, t?: TFunction): string {
  return translate(t, `components.personalityLabels.categories.${key}`)
}

export function axisLabel(key: keyof PersonalityAxesDto, t?: TFunction): string {
  return translate(t, `components.personalityLabels.axes.${key}`)
}

export function labelGoodDay(t?: TFunction): string {
  return translate(t, 'components.personalityLabels.goodDay')
}

export function labelBadDay(t?: TFunction): string {
  return translate(t, 'components.personalityLabels.badDay')
}

export function labelSucceedThrough(t?: TFunction): string {
  return translate(t, 'components.personalityLabels.succeedThrough')
}

export function labelTopStrength(t?: TFunction): string {
  return translate(t, 'components.personalityLabels.topStrength')
}

export function labelPronounced(t?: TFunction): string {
  return translate(t, 'components.personalityLabels.pronounced')
}

export function labelYouOnScale(t?: TFunction): string {
  return translate(t, 'components.personalityLabels.youOnScale')
}
