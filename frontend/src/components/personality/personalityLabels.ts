import type {PersonalityAxesDto} from '../../api/types'

export const CATEGORY_LABELS: Record<string, string> = {
  connections: 'Связи',
  creativity: 'Креативность',
  drive: 'Драйв',
  thinking: 'Мышление',
}

export const CATEGORY_ORDER = ['connections', 'creativity', 'drive', 'thinking'] as const

export const AXIS_LABELS: Record<keyof PersonalityAxesDto, string> = {
  axisDominance: 'Доминантность',
  axisInfluence: 'Влияние',
  axisStability: 'Стабильность',
  axisIntegrity: 'Добросовестность',
  axisAutonomy: 'Автономность',
  axisPace: 'Темп',
}

export const AXIS_KEYS = Object.keys(AXIS_LABELS) as (keyof PersonalityAxesDto)[]

export const LABEL_GOOD_DAY = 'В хороший день'
export const LABEL_BAD_DAY = 'В плохой день'
export const LABEL_SUCCEED_THROUGH = 'Вы добиваетесь успеха через'
export const LABEL_TOP_STRENGTH = 'Главная сила'
export const LABEL_PRONOUNCED = 'Выраженная'
export const LABEL_YOU_ON_SCALE = 'Вы'
