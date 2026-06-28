import {useNavigate, useParams} from 'react-router-dom'
import {useCallback, useEffect, useMemo, useState} from 'react'
import {completeSurvey, fetchSurvey, saveSurveyAnswers, startSurvey} from '../../api/seekerApi'
import type {SurveyDetailDto} from '../../api/types'
import {
    isSurveyComplete,
    parseAnswersJson,
    parseSurveyDefinition,
    SurveyQuestionRenderer,
} from '../../components/survey/SurveyQuestionRenderer'
import {Alert} from '../../components/ui/Alert'
import {Button} from '../../components/ui/Button'
import {PageHeader} from '../../components/ui/PageHeader'
import {Spinner} from '../../components/Spinner'

const SCALE_PAGE_SIZE = 8

const TEST_TITLES: Record<string, string> = {
  core: 'Тест 1',
  '64qn': 'Тест 2',
}

function isCoreStepReview(survey: SurveyDetailDto): boolean {
  return (
    survey.groupCode === 'core' &&
    survey.status === 'COMPLETED' &&
    survey.stepNumber != null &&
    survey.stepTotal != null &&
    survey.stepNumber < survey.stepTotal
  )
}

function shouldRedirectCompleted(survey: SurveyDetailDto): boolean {
  if (survey.groupCode === '64qn') return true
  if (survey.groupCode !== 'core') return true
  if (survey.stepNumber == null || survey.stepTotal == null) return true
  return survey.stepNumber >= survey.stepTotal
}

export function SeekerTestTakePage() {
  const {surveyId} = useParams()
  const navigate = useNavigate()
  const id = Number(surveyId)
  const [survey, setSurvey] = useState<SurveyDetailDto | null>(null)
  const [answers, setAnswers] = useState<Record<string, unknown>>({})
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [scalePage, setScalePage] = useState(0)

  const load = useCallback(async () => {
    if (!Number.isFinite(id)) {
      setError('Некорректный идентификатор теста')
      return
    }
    let detail = await fetchSurvey(id)
    if (detail.locked) {
      setError('Сначала завершите предыдущий тест')
      setSurvey(detail)
      return
    }
    if (detail.status === 'COMPLETED' && shouldRedirectCompleted(detail)) {
      navigate('/seeker/personality/tests', {replace: true})
      return
    }
    if (detail.status === 'NOT_STARTED') {
      detail = await startSurvey(id)
    }
    setSurvey(detail)
    setAnswers(parseAnswersJson(detail.answersJson ?? null))
    setScalePage(0)
  }, [id, navigate])

  useEffect(() => {
    void load().catch((e: Error) => setError(e.message))
  }, [load])

  const definition = useMemo(
    () => (survey != null ? parseSurveyDefinition(survey.questionsJson) : null),
    [survey],
  )

  const scalePageCount =
    definition?.type === 'scale_0_4'
      ? Math.ceil((definition.questions?.length ?? 0) / SCALE_PAGE_SIZE)
      : 1

  const isCoreTest = survey?.groupCode === 'core'
  const isLastCoreStep =
    isCoreTest &&
    survey.stepNumber != null &&
    survey.stepTotal != null &&
    survey.stepNumber >= survey.stepTotal
  const isScaleTest = definition?.type === 'scale_0_4'
  const onLastScalePage = !isScaleTest || scalePage >= scalePageCount - 1

  async function persistAnswers(next: Record<string, unknown>) {
    setAnswers(next)
    if (survey == null) return
    try {
      await saveSurveyAnswers(survey.id, next)
    } catch {
      // autosave is best-effort
    }
  }

  async function handleAdvance() {
    if (survey == null || definition == null) return
    if (!isSurveyComplete(definition, answers)) {
      setError('Ответьте на все вопросы, прежде чем продолжить')
      return
    }
    setSubmitting(true)
    setError(null)
    try {
      if (isCoreStepReview(survey) && survey.nextSurveyId != null) {
        navigate(`/seeker/personality/tests/${survey.nextSurveyId}`)
        return
      }
      const result = await completeSurvey(survey.id, answers)
      if (result.nextSurveyId != null) {
        navigate(`/seeker/personality/tests/${result.nextSurveyId}`)
      } else {
        navigate('/seeker/personality/tests')
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Не удалось сохранить ответы')
    } finally {
      setSubmitting(false)
    }
  }

  async function handleBack() {
    if (survey?.prevSurveyId == null) return
    setError(null)
    try {
      await saveSurveyAnswers(survey.id, answers)
    } catch {
      // best-effort before leaving step
    }
    navigate(`/seeker/personality/tests/${survey.prevSurveyId}`)
  }

  if (error != null && survey == null) {
    return <Alert variant="error">{error}</Alert>
  }
  if (survey == null || definition == null) {
    if (survey?.locked === true) {
      return (
        <div className="flex flex-col gap-4">
          <PageHeader
            title="Тест заблокирован"
            backTo="/seeker/personality/tests"
            backLabel="К тестам"
          />
          <Alert variant="error">{error ?? 'Сначала завершите предыдущий тест'}</Alert>
        </div>
      )
    }
    return (
      <div className="flex justify-center py-16">
        <Spinner />
      </div>
    )
  }

  const complete = isSurveyComplete(definition, answers)
  const testTitle = TEST_TITLES[survey.groupCode] ?? survey.name

  const hasCoreSteps =
    isCoreTest && survey.stepNumber != null && survey.stepTotal != null
  const hasScalePages = isScaleTest && scalePageCount > 1
  const showFinishAction =
    (isCoreTest && isLastCoreStep) || (isScaleTest && onLastScalePage)
  const showNavBar = hasCoreSteps || hasScalePages || showFinishAction

  function handlePrimary() {
    if (isScaleTest && !onLastScalePage) {
      setScalePage((p) => p + 1)
      return
    }
    void handleAdvance()
  }

  const primaryLabel = submitting
    ? 'Сохранение…'
    : showFinishAction
      ? 'Завершить'
      : 'Далее'
  const primaryDisabled = showFinishAction
    ? !complete || submitting
    : hasCoreSteps
      ? !complete || submitting
      : submitting

  const progressLabel = hasCoreSteps
    ? `Часть ${survey.stepNumber} / ${survey.stepTotal}`
    : hasScalePages
      ? `Страница ${scalePage + 1} / ${scalePageCount}`
      : null

  return (
    <div className="flex flex-col gap-6">
      <PageHeader title={testTitle} backTo="/seeker/personality/tests" backLabel="К тестам" />

      {error != null && <Alert variant="error">{error}</Alert>}

      {showNavBar && (
        <div className="sticky top-0 z-10 rounded-2xl border border-brand-100 bg-white/95 p-4 card-shadow backdrop-blur-sm">
          <div className="flex items-center justify-between gap-4">
            {progressLabel != null && (
              <span className="text-sm font-medium text-stone-600">{progressLabel}</span>
            )}
            <div className="ml-auto flex shrink-0 gap-2">
              {(hasCoreSteps || hasScalePages) && (
                <Button
                  size="sm"
                  variant="secondary"
                  disabled={
                    hasCoreSteps ? survey.stepNumber! <= 1 || submitting : scalePage === 0
                  }
                  onClick={() =>
                    hasCoreSteps ? void handleBack() : setScalePage((p) => p - 1)
                  }
                >
                  Назад
                </Button>
              )}
              <Button size="sm" disabled={primaryDisabled} onClick={handlePrimary}>
                {primaryLabel}
              </Button>
            </div>
          </div>
        </div>
      )}

      <SurveyQuestionRenderer
        definition={definition}
        answers={answers}
        onChange={(next) => void persistAnswers(next)}
        pageIndex={isScaleTest ? scalePage : undefined}
        pageSize={isScaleTest ? SCALE_PAGE_SIZE : undefined}
      />
    </div>
  )
}
