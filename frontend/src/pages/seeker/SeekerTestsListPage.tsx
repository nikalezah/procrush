import { Link } from 'react-router-dom'
import { useEffect, useState } from 'react'
import { fetchSurveyGroups } from '../../api/seekerApi'
import type { SurveyGroupsResponseDto, SurveyStatus } from '../../api/types'

const STATUS_LABELS: Record<SurveyStatus, string> = {
  NOT_STARTED: 'Не начат',
  IN_PROGRESS: 'В процессе',
  COMPLETED: 'Пройден',
}

const TEST_DESCRIPTIONS: Record<string, string> = {
  core: 'Комплексная оценка личности, мотивации и командных ролей',
  '64qn': 'Оценка личностных особенностей по 64 утверждениям',
}

function StatusBadge({ status }: { status: SurveyStatus }) {
  const colors =
    status === 'COMPLETED'
      ? 'bg-green-100 text-green-800'
      : status === 'IN_PROGRESS'
        ? 'bg-blue-100 text-blue-800'
        : 'bg-neutral-100 text-neutral-700'
  return <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${colors}`}>{STATUS_LABELS[status]}</span>
}

function LockedBadge() {
  return (
    <span className="rounded-full bg-neutral-200 px-2 py-0.5 text-xs font-medium text-neutral-600">
      Заблокирован
    </span>
  )
}

export function SeekerTestsListPage() {
  const [data, setData] = useState<SurveyGroupsResponseDto | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    void fetchSurveyGroups()
      .then(setData)
      .catch((e: Error) => setError(e.message))
  }, [])

  if (error != null) return <p className="text-sm text-red-600">{error}</p>
  if (data == null) return <p className="text-sm text-neutral-500">Загрузка…</p>

  return (
    <div className="flex flex-col gap-6">
      <div>
        <Link to="/seeker/personality" className="text-sm text-neutral-600 underline">
          ← Личностные характеристики
        </Link>
        <h1 className="mt-2 text-2xl font-semibold">Личностные тесты</h1>
        <p className="mt-1 text-sm text-neutral-600">
          Пройдено тестов: {data.testsCompleted} / {data.testsTotal}
        </p>
      </div>

      <div className="flex flex-col gap-4">
        {data.groups.map((group) => {
          const canStart = !group.locked && group.status !== 'COMPLETED' && group.entrySurveyId != null
          return (
            <div
              key={group.code}
              className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-neutral-200 p-5"
            >
              <div>
                <div className="flex items-center gap-2">
                  <h2 className="text-lg font-semibold">{group.name}</h2>
                  {group.locked ? <LockedBadge /> : <StatusBadge status={group.status} />}
                </div>
                <p className="mt-1 text-sm text-neutral-600">
                  {TEST_DESCRIPTIONS[group.code] ?? ''}
                </p>
              </div>
              {canStart && (
                <Link
                  to={`/seeker/personality/tests/${group.entrySurveyId}`}
                  className="rounded-lg bg-neutral-900 px-4 py-2 text-sm font-medium text-white"
                >
                  {group.status === 'IN_PROGRESS' ? 'Продолжить' : 'Начать'}
                </Link>
              )}
            </div>
          )
        })}
      </div>
    </div>
  )
}
