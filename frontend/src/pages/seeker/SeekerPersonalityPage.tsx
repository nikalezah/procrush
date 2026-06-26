import {Link} from 'react-router-dom'
import {useCallback, useEffect, useState} from 'react'
import {
    fetchPersonalityPreview,
    subscribePersonalityStatusEvents,
    triggerPersonalityGeneration
} from '../../api/seekerApi'
import type {PersonalityPreviewDto} from '../../api/types'
import {FormSection} from '../../components/FormSection'
import {DiscHexagonChart} from '../../components/personality/DiscHexagonChart'
import {PersonalityCategoryTabs} from '../../components/personality/PersonalityCategoryTabs'
import {SuperpowersAndTalentsSection} from '../../components/personality/SuperpowersAndTalentsSection'
import {Alert} from '../../components/ui/Alert'
import {Button} from '../../components/ui/Button'
import {PageHeader} from '../../components/ui/PageHeader'
import {Spinner} from '../../components/Spinner'

const AXIS_LABELS: Record<string, string> = {
  axisDominance: 'Доминантность',
  axisInfluence: 'Влияние',
  axisStability: 'Стабильность',
  axisIntegrity: 'Добросовестность',
  axisAutonomy: 'Автономность',
  axisPace: 'Темп',
}

export function SeekerPersonalityPage() {
  const [data, setData] = useState<PersonalityPreviewDto | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [retrying, setRetrying] = useState(false)

  const load = useCallback(async () => {
    const preview = await fetchPersonalityPreview()
    setData(preview)
    return preview
  }, [])

  useEffect(() => {
    void load().catch((e: Error) => setError(e.message))
  }, [load])

  useEffect(() => {
    if (data == null) return
    if (data.testsCompleted < data.testsTotal) return
    if (data.status !== 'PROCESSING') return

    const refresh = () => {
      void load().catch((e: Error) => setError(e.message))
    }

    const unsubscribe = subscribePersonalityStatusEvents(refresh, refresh)
    return unsubscribe
  }, [data?.status, data?.testsCompleted, data?.testsTotal, load])

  async function handleRetry() {
    setRetrying(true)
    setError(null)
    setData((prev) =>
      prev != null
        ? { ...prev, status: 'PROCESSING', generationError: null }
        : prev,
    )
    try {
      await triggerPersonalityGeneration()
      await load()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Не удалось запустить генерацию')
    } finally {
      setRetrying(false)
    }
  }

  if (error != null && data == null) {
    return <p className="text-sm text-red-600">{error}</p>
  }
  if (data == null) {
    return (
      <div className="flex justify-center py-16">
        <Spinner />
      </div>
    )
  }

  const testsBanner = (
    <Alert
      variant="warning"
      title={`Тесты: ${data.testsCompleted}/${data.testsTotal}`}
      action={
        <Link to="/seeker/personality/tests">
          <Button size="sm">
            {data.testsCompleted >= data.testsTotal ? 'Смотреть' : 'Пройти'}
          </Button>
        </Link>
      }
    >
      Для точного мэтчинга завершите все методики в каждом тесте
    </Alert>
  )

  if (data.status === 'NOT_READY') {
    return (
      <div className="flex flex-col gap-6">
        <PageHeader
          title="Ваша личность ✨"
          subtitle="Пройдите тесты, чтобы мы узнали вас лучше"
        />
        {testsBanner}
      </div>
    )
  }

  if (data.status === 'PROCESSING') {
    return (
      <div className="flex flex-col gap-6">
        <PageHeader
          title="Ваша личность ✨"
          subtitle="Формируем ваш уникальный профиль"
        />
        {testsBanner}
        <div className="flex flex-col items-center gap-4 rounded-[var(--radius-card)] border border-brand-100 bg-white p-10 card-shadow">
          <Spinner />
          <p className="text-center text-sm text-stone-600">
            Анализируем ваши ответы… Это может занять несколько минут
          </p>
        </div>
      </div>
    )
  }

  if (data.status === 'FAILED') {
    return (
      <div className="flex flex-col gap-6">
        <PageHeader title="Ваша личность ✨" />
        {testsBanner}
        <Alert variant="error" title="Не удалось сформировать профиль">
          {data.generationError ?? 'Произошла ошибка при генерации'}
          <div className="mt-3">
            <Button size="sm" onClick={() => void handleRetry()} disabled={retrying}>
              {retrying ? 'Запуск…' : 'Повторить'}
            </Button>
          </div>
        </Alert>
        {error != null && <p className="text-sm text-red-600">{error}</p>}
      </div>
    )
  }

  const axes = [
    data.axisDominance ?? 0,
    data.axisInfluence ?? 0,
    data.axisStability ?? 0,
    data.axisIntegrity ?? 0,
    data.axisAutonomy ?? 0,
    data.axisPace ?? 0,
  ]
  const axisKeys = Object.keys(AXIS_LABELS)
  const categories = data.categories ?? []
  const superpowersAndTalents = data.superpowersAndTalents ?? []
  const energySources = data.energySources
  const stopFactors = data.stopFactors
  const showEnergyStopSideBySide = energySources != null && stopFactors != null

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="Ваша личность ✨"
        subtitle="Уникальный профиль для точного подбора работы"
      />
      {testsBanner}
      <FormSection title={data.title ?? ''} description={data.description ?? undefined}>
        {data.profile != null && (
          <p className="text-sm leading-relaxed text-stone-700">{data.profile}</p>
        )}
        {(data.autonomy != null || data.thinkingStyle != null || data.burnoutRisk != null) && (
          <dl className="grid gap-4 sm:grid-cols-3">
            {data.autonomy != null && (
              <div className="rounded-xl bg-brand-50/50 p-3">
                <dt className="text-sm font-semibold text-stone-800">Автономность</dt>
                <dd className="mt-1 text-sm text-stone-600">{data.autonomy}</dd>
              </div>
            )}
            {data.thinkingStyle != null && (
              <div className="rounded-xl bg-brand-50/50 p-3">
                <dt className="text-sm font-semibold text-stone-800">Стиль мышления</dt>
                <dd className="mt-1 text-sm text-stone-600">{data.thinkingStyle}</dd>
              </div>
            )}
            {data.burnoutRisk != null && (
              <div className="rounded-xl bg-brand-50/50 p-3">
                <dt className="text-sm font-semibold text-stone-800">Риск выгорания</dt>
                <dd className="mt-1 text-sm text-stone-600">{data.burnoutRisk}</dd>
              </div>
            )}
          </dl>
        )}
      </FormSection>
      {superpowersAndTalents.length > 0 && (
        <SuperpowersAndTalentsSection items={superpowersAndTalents} />
      )}
      {categories.length > 0 && <PersonalityCategoryTabs categories={categories} />}
      <FormSection title="Профиль DISC">
        <DiscHexagonChart
          labels={axisKeys.map((key) => AXIS_LABELS[key])}
          values={axes}
        />
      </FormSection>
      {(energySources != null || stopFactors != null) && (
        <div
          className={
            showEnergyStopSideBySide
              ? 'flex flex-col gap-6 md:flex-row'
              : 'flex flex-col gap-6'
          }
        >
          {energySources != null && (
            <div className={showEnergyStopSideBySide ? 'md:min-w-0 md:flex-1' : undefined}>
              <FormSection title={energySources.title}>
                <ul className="flex flex-col gap-3">
                  {energySources.items.map((item) => (
                    <li key={item.title} className="rounded-xl bg-brand-50/40 p-3">
                      <p className="text-sm font-semibold text-stone-800">{item.title}</p>
                      <p className="text-sm text-stone-600">{item.description}</p>
                    </li>
                  ))}
                </ul>
              </FormSection>
            </div>
          )}
          {stopFactors != null && (
            <div className={showEnergyStopSideBySide ? 'md:min-w-0 md:flex-1' : undefined}>
              <FormSection title={stopFactors.title}>
                <ul className="flex flex-col gap-3">
                  {stopFactors.items.map((item) => (
                    <li key={item.title} className="rounded-xl bg-brand-50/40 p-3">
                      <p className="text-sm font-semibold text-stone-800">{item.title}</p>
                      <p className="text-sm text-stone-600">{item.description}</p>
                    </li>
                  ))}
                </ul>
              </FormSection>
            </div>
          )}
        </div>
      )}
    </div>
  )
}
