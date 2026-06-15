import { Link } from 'react-router-dom'
import { useEffect, useState } from 'react'
import { fetchSeekerDashboard } from '../../api/seekerApi'
import type { SeekerDashboardDto } from '../../api/types'
import { MatchScoreBadge } from '../../components/MatchScoreBadge'

export function SeekerDashboardPage() {
  const [data, setData] = useState<SeekerDashboardDto | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    void fetchSeekerDashboard()
      .then(setData)
      .catch((e: Error) => setError(e.message))
  }, [])

  if (error != null) return <p className="text-sm text-red-600">{error}</p>
  if (data == null) return <p className="text-sm text-neutral-500">Загрузка…</p>

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="text-2xl font-semibold">Дашборд</h1>
        <p className="mt-1 text-sm text-neutral-600">Обзор вашего профиля и рекомендаций</p>
      </div>
      <div className="grid gap-4 sm:grid-cols-3">
        <div className="rounded-xl border border-neutral-200 bg-white p-4">
          <p className="text-sm text-neutral-600">Заполнение профиля</p>
          <p className="mt-1 text-2xl font-semibold">{data.profileCompletionPercent}%</p>
          <Link to="/seeker/profile" className="mt-2 inline-block text-sm text-neutral-900 underline">
            Заполнить профиль
          </Link>
        </div>
        <div className="rounded-xl border border-neutral-200 bg-white p-4">
          <p className="text-sm text-neutral-600">Опыт работы</p>
          <p className="mt-1 text-2xl font-semibold">{data.experienceCount}</p>
        </div>
        <div className="rounded-xl border border-neutral-200 bg-white p-4">
          <p className="text-sm text-neutral-600">Желаемые должности</p>
          <p className="mt-1 text-2xl font-semibold">{data.desiredPositionsCount}</p>
        </div>
      </div>
      <div className="rounded-xl border border-amber-200 bg-amber-50 p-4">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <p className="font-medium text-amber-900">Личностное тестирование</p>
            <p className="mt-1 text-sm text-amber-800">
              Пройдите 3 теста для более точного подбора вакансий
            </p>
          </div>
          <Link
            to="/seeker/personality/tests"
            className="rounded-lg bg-amber-900 px-4 py-2 text-sm font-medium text-white"
          >
            Пройти тесты
          </Link>
        </div>
      </div>
      <section>
        <div className="mb-3 flex items-center justify-between">
          <h2 className="text-lg font-semibold">Рекомендации</h2>
          <Link to="/seeker/positions" className="text-sm text-neutral-700 underline">
            Все вакансии
          </Link>
        </div>
        <div className="flex flex-col gap-3">
          {data.recommendationsPreview.map((job) => (
            <article
              key={job.id}
              className="flex flex-col gap-3 rounded-xl border border-neutral-200 bg-white p-4 sm:flex-row sm:items-start sm:justify-between"
            >
              <div>
                <h3 className="font-medium">{job.positionName}</h3>
                <p className="text-sm text-neutral-600">{job.companyName}</p>
                <p className="mt-2 text-sm text-neutral-700">{job.description}</p>
              </div>
              <MatchScoreBadge
                score={job.matchScoreDisplay}
                testsCompleted={job.testsCompleted}
                isScoreReduced={job.isScoreReduced}
              />
            </article>
          ))}
        </div>
      </section>
    </div>
  )
}
