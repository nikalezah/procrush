import { Link } from 'react-router-dom'
import { useCallback, useEffect, useState } from 'react'
import { fetchPersonalityPreview, triggerPersonalityGeneration } from '../../api/seekerApi'
import type { PersonalityPreviewDto } from '../../api/types'
import { FormSection } from '../../components/FormSection'
import { DiscHexagonChart } from '../../components/personality/DiscHexagonChart'
import { PersonalityCategoryTabs } from '../../components/personality/PersonalityCategoryTabs'
import { SuperpowersAndTalentsSection } from '../../components/personality/SuperpowersAndTalentsSection'

const AXIS_LABELS: Record<string, string> = {
  axisDominance: 'Доминантность',
  axisInfluence: 'Влияние',
  axisStability: 'Стабильность',
  axisIntegrity: 'Добросовестность',
  axisAutonomy: 'Автономность',
  axisPace: 'Темп',
}

const POLL_INTERVAL_MS = 4000

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
    if (data.status === 'READY' || data.status === 'NOT_READY' || data.status === 'FAILED') return

    const interval = window.setInterval(() => {
      void load().catch((e: Error) => setError(e.message))
    }, POLL_INTERVAL_MS)
    return () => window.clearInterval(interval)
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
  if (data == null) return <p className="text-sm text-neutral-500">Загрузка…</p>

  const testsBanner = (
    <div className="rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <p>
          Пройдено тестов: {data.testsCompleted}/{data.testsTotal}. Для точного мэтчинга завершите все
          методики в каждом тесте.
        </p>
        <Link
          to="/seeker/personality/tests"
          className="rounded-lg bg-amber-900 px-4 py-2 text-sm font-medium text-white"
        >
          {data.testsCompleted >= data.testsTotal ? 'Смотреть тесты' : 'Пройти тесты'}
        </Link>
      </div>
    </div>
  )

  if (data.status === 'NOT_READY') {
    return (
      <div className="flex flex-col gap-6">
        <div>
          <h1 className="text-2xl font-semibold">Личностные характеристики</h1>
          <p className="mt-1 text-sm text-neutral-600">
            Пройдите все тесты для формирования профиля
          </p>
        </div>
        {testsBanner}
      </div>
    )
  }

  if (data.status === 'PROCESSING') {
    return (
      <div className="flex flex-col gap-6">
        <div>
          <h1 className="text-2xl font-semibold">Личностные характеристики</h1>
          <p className="mt-1 text-sm text-neutral-600">
            Все тесты пройдены — формируем ваш профиль
          </p>
        </div>
        {testsBanner}
        <div className="flex flex-col items-center gap-3 rounded-xl border border-neutral-200 bg-neutral-50 p-8">
          <div
            className="h-8 w-8 animate-spin rounded-full border-2 border-neutral-300 border-t-neutral-900"
            aria-hidden
          />
          <p className="text-sm text-neutral-700">
            Формируем ваш личностный профиль… Это может занять время.
          </p>
        </div>
      </div>
    )
  }

  if (data.status === 'FAILED') {
    return (
      <div className="flex flex-col gap-6">
        <div>
          <h1 className="text-2xl font-semibold">Личностные характеристики</h1>
        </div>
        {testsBanner}
        <div className="rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-900">
          <p>{data.generationError ?? 'Не удалось сформировать профиль'}</p>
          <button
            type="button"
            onClick={() => void handleRetry()}
            disabled={retrying}
            className="mt-3 rounded-lg bg-red-900 px-4 py-2 text-sm font-medium text-white disabled:opacity-50"
          >
            {retrying ? 'Запуск…' : 'Повторить'}
          </button>
        </div>
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
      <div>
        <h1 className="text-2xl font-semibold">Личностные характеристики</h1>
        <p className="mt-1 text-sm text-neutral-600">Ваш сформированный личностный профиль</p>
      </div>
      {testsBanner}
      <FormSection title={data.title ?? ''} description={data.description ?? undefined}>
        {data.profile != null && (
          <p className="text-sm text-neutral-700">{data.profile}</p>
        )}
        {(data.autonomy != null || data.thinkingStyle != null || data.burnoutRisk != null) && (
          <dl className="grid gap-4 sm:grid-cols-3">
            {data.autonomy != null && (
              <div>
                <dt className="font-medium text-sm">Автономность</dt>
                <dd className="mt-1 text-sm text-neutral-600">{data.autonomy}</dd>
              </div>
            )}
            {data.thinkingStyle != null && (
              <div>
                <dt className="font-medium text-sm">Стиль мышления</dt>
                <dd className="mt-1 text-sm text-neutral-600">{data.thinkingStyle}</dd>
              </div>
            )}
            {data.burnoutRisk != null && (
              <div>
                <dt className="font-medium text-sm">Риск выгорания</dt>
                <dd className="mt-1 text-sm text-neutral-600">{data.burnoutRisk}</dd>
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
                    <li key={item.title}>
                      <p className="font-medium text-sm">{item.title}</p>
                      <p className="text-sm text-neutral-600">{item.description}</p>
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
                    <li key={item.title}>
                      <p className="font-medium text-sm">{item.title}</p>
                      <p className="text-sm text-neutral-600">{item.description}</p>
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
