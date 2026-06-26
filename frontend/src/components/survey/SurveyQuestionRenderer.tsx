import type {SurveyQuestionsDefinition} from '../../api/types'

type Answers = Record<string, unknown>

interface Props {
  definition: SurveyQuestionsDefinition
  answers: Answers
  onChange: (answers: Answers) => void
  pageIndex?: number
  pageSize?: number
}

function parseSelected(value: unknown): number[] {
  if (!Array.isArray(value)) return []
  return value.filter((v): v is number => typeof v === 'number')
}

function toggleSelection(current: number[], id: number, max: number): number[] {
  if (current.includes(id)) return current.filter((x) => x !== id)
  if (current.length >= max) return current
  return [...current, id]
}

function getBlockAnswers(answers: Answers, key: string): Record<string, number> {
  const block = answers[key]
  if (block == null || typeof block !== 'object' || Array.isArray(block)) return {}
  return block as Record<string, number>
}

function setBlockPoint(
  answers: Answers,
  key: string,
  optionId: number,
  value: number,
  maxPer: number,
  total: number,
  onChange: (a: Answers) => void,
) {
  const block = getBlockAnswers(answers, key)
  const clamped = Math.min(maxPer, Math.max(0, value))
  const otherSum = Object.entries(block).reduce((sum, [k, v]) => {
    if (k === String(optionId)) return sum
    return sum + (v ?? 0)
  }, 0)
  const finalValue = Math.min(clamped, Math.max(0, total - otherSum))
  onChange({ ...answers, [key]: { ...block, [String(optionId)]: finalValue } })
}

interface NumberOptionButtonsProps {
  max: number
  value: number
  maxAllowed?: number
  ariaLabel?: string
  onChange: (value: number) => void
}

function NumberOptionButtons({ max, value, maxAllowed = max, ariaLabel, onChange }: NumberOptionButtonsProps) {
  return (
    <div className="flex shrink-0 gap-1" role="group" aria-label={ariaLabel}>
      {Array.from({ length: max + 1 }, (_, pts) => {
        const disabled = pts > maxAllowed
        const selected = value === pts
        return (
          <button
            key={pts}
            type="button"
            disabled={disabled}
            aria-pressed={selected}
            onClick={() => onChange(pts)}
            className={[
              'flex h-9 w-9 items-center justify-center rounded-md text-sm font-medium transition-colors',
              selected
                ? 'gradient-brand text-white shadow-sm'
                : disabled
                  ? 'cursor-not-allowed bg-stone-100 text-stone-300'
                  : 'border border-brand-200 bg-white text-stone-700 hover:border-brand-300 hover:bg-brand-50',
            ].join(' ')}
          >
            {pts}
          </button>
        )
      })}
    </div>
  )
}

interface PointAllocationRowProps {
  label: string
  value: number
  maxPer: number
  maxAllowed: number
  onChange: (value: number) => void
}

function PointAllocationRow({ label, value, maxPer, maxAllowed, onChange }: PointAllocationRowProps) {
  return (
    <div className="flex flex-col gap-2 rounded-lg border border-brand-100 bg-brand-50/60 px-3 py-2.5 sm:flex-row sm:items-center sm:justify-between sm:gap-3">
      <span className="flex-1 text-sm leading-snug">{label}</span>
      <NumberOptionButtons
        max={maxPer}
        value={value}
        maxAllowed={maxAllowed}
        ariaLabel={label}
        onChange={onChange}
      />
    </div>
  )
}

export function SurveyQuestionRenderer({ definition, answers, onChange, pageIndex = 0, pageSize }: Props) {
  const { type, instruction } = definition

  if (type === 'open_questions') {
    return (
      <div className="flex flex-col gap-4">
        <p className="text-sm text-stone-600">{instruction}</p>
        {definition.questions?.map((q) => (
          <label key={q.id} className="flex flex-col gap-1">
            <span className="text-sm font-medium">{q.text}</span>
            <textarea
              rows={3}
              value={String(answers[String(q.id)] ?? '')}
              onChange={(e) => onChange({ ...answers, [String(q.id)]: e.target.value })}
              className="rounded-lg border border-brand-300 px-3 py-2 text-sm"
            />
          </label>
        ))}
      </div>
    )
  }

  if (type === 'multi_select') {
    const key = definition.answerKey ?? 'selected'
    const selected = parseSelected(answers[key])
    const max = definition.maxSelections ?? selected.length
    const options = definition.options ?? []
    return (
      <div className="flex flex-col gap-4">
        <p className="text-sm text-stone-600">{instruction}</p>
        <p className="text-xs text-stone-500">
          Выбрано: {selected.length} / {max}
        </p>
        <div className="grid gap-2 sm:grid-cols-2">
          {options.map((opt) => (
            <label key={opt.id} className="flex items-center gap-2 rounded-lg border border-brand-200 p-2 text-sm">
              <input
                type="checkbox"
                checked={selected.includes(opt.id)}
                onChange={() => onChange({ ...answers, [key]: toggleSelection(selected, opt.id, max) })}
              />
              {opt.label}
            </label>
          ))}
        </div>
      </div>
    )
  }

  if (type === 'binary_choice') {
    return (
      <div className="flex flex-col gap-4">
        <p className="text-sm text-stone-600">{instruction}</p>
        {definition.questions?.map((q) => (
          <fieldset key={q.id} className="rounded-lg border border-brand-200 p-3">
            <legend className="px-1 text-sm font-medium">Вопрос {q.id}</legend>
            <div className="mt-2 flex flex-col gap-2">
              {[1, 2].map((choice) => (
                <label key={choice} className="flex items-center gap-2 text-sm">
                  <input
                    type="radio"
                    name={`q-${q.id}`}
                    checked={answers[String(q.id)] === choice}
                    onChange={() => onChange({ ...answers, [String(q.id)]: choice })}
                  />
                  {choice === 1 ? q.option1 : q.option2}
                </label>
              ))}
            </div>
          </fieldset>
        ))}
      </div>
    )
  }

  if (type === 'scale_0_4') {
    const all = definition.questions ?? []
    const start = pageSize != null ? pageIndex * pageSize : 0
    const items = pageSize != null ? all.slice(start, start + pageSize) : all
    return (
      <div className="flex flex-col gap-4">
        <p className="text-sm text-stone-600">{instruction}</p>
        {items.map((q) => {
          const current = typeof answers[String(q.id)] === 'number' ? (answers[String(q.id)] as number) : -1
          return (
            <div
              key={q.id}
              className="flex flex-col gap-2 rounded-lg border border-brand-100 bg-brand-50/60 px-3 py-2.5 sm:flex-row sm:items-center sm:justify-between sm:gap-3"
            >
              <span className="flex-1 text-sm leading-snug">{q.text}</span>
              <NumberOptionButtons
                max={4}
                value={current}
                ariaLabel={q.text}
                onChange={(v) => onChange({ ...answers, [String(q.id)]: v })}
              />
            </div>
          )
        })}
      </div>
    )
  }

  if (type === 'allocate_points' || type === 'belbin_matrix') {
    const total = definition.totalPoints ?? 10
    const maxPer = definition.maxPerOption ?? 5
    const prefix = type === 'belbin_matrix' ? 'section_' : 'q'
    return (
      <div className="flex flex-col gap-6">
        <p className="text-sm text-stone-600">{instruction}</p>
        {(definition.questions ?? []).map((q) => {
          const key = `${prefix}${q.id}`
          const block = getBlockAnswers(answers, key)
          const sum = Object.values(block).reduce((a, b) => a + (b ?? 0), 0)
          return (
            <fieldset key={q.id} className="rounded-lg border border-brand-200 p-3">
              <legend className="px-1 text-sm font-medium">{q.text}</legend>
              <p
                className={`mt-1 text-xs ${sum === total ? 'font-medium text-green-700' : 'text-stone-500'}`}
              >
                {sum === total
                  ? `Распределено ${total} из ${total} баллов`
                  : `Распределено ${sum} из ${total} (осталось ${total - sum})`}
              </p>
              <div className="mt-3 flex flex-col gap-2">
                {(q.options ?? []).map((opt) => {
                  const current = block[String(opt.id)] ?? 0
                  const otherSum = sum - current
                  const fieldMax = Math.min(maxPer, total - otherSum)
                  return (
                    <PointAllocationRow
                      key={opt.id}
                      label={opt.label}
                      value={current}
                      maxPer={maxPer}
                      maxAllowed={fieldMax}
                      onChange={(pts) => setBlockPoint(answers, key, opt.id, pts, maxPer, total, onChange)}
                    />
                  )
                })}
              </div>
            </fieldset>
          )
        })}
      </div>
    )
  }

  return <p className="text-sm text-red-600">Неподдерживаемый тип опроса: {type}</p>
}

export function isSurveyComplete(definition: SurveyQuestionsDefinition, answers: Answers): boolean {
  try {
    switch (definition.type) {
      case 'open_questions':
        return (definition.questions ?? []).every((q) => String(answers[String(q.id)] ?? '').trim().length > 0)
      case 'multi_select': {
        const key = definition.answerKey ?? 'selected'
        const n = parseSelected(answers[key]).length
        return n === (definition.minSelections ?? n)
      }
      case 'binary_choice':
        return (definition.questions ?? []).every((q) => answers[String(q.id)] === 1 || answers[String(q.id)] === 2)
      case 'scale_0_4':
        return (definition.questions ?? []).every(
          (q) => typeof answers[String(q.id)] === 'number' && (answers[String(q.id)] as number) >= 0,
        )
      case 'allocate_points':
      case 'belbin_matrix': {
        const total = definition.totalPoints ?? 10
        const prefix = definition.type === 'belbin_matrix' ? 'section_' : 'q'
        return (definition.questions ?? []).every((q) => {
          const block = getBlockAnswers(answers, `${prefix}${q.id}`)
          const sum = Object.values(block).reduce((a, b) => a + (b ?? 0), 0)
          return sum === total
        })
      }
      default:
        return false
    }
  } catch {
    return false
  }
}

export function parseSurveyDefinition(json: string): SurveyQuestionsDefinition {
  return JSON.parse(json) as SurveyQuestionsDefinition
}

export function parseAnswersJson(json: string | null): Answers {
  if (json == null || json === '') return {}
  try {
    return JSON.parse(json) as Answers
  } catch {
    return {}
  }
}
