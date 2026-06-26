import {Link} from 'react-router-dom'
import {useEffect, useState} from 'react'
import {fetchSurveyGroups} from '../../api/seekerApi'
import type {SurveyGroupsResponseDto, SurveyStatus} from '../../api/types'
import {Button} from '../../components/ui/Button'
import {Card} from '../../components/ui/Card'
import {PageHeader} from '../../components/ui/PageHeader'
import {Spinner} from '../../components/Spinner'

const STATUS_LABELS: Record<SurveyStatus, string> = {
  NOT_STARTED: 'Не начат',
  IN_PROGRESS: 'В процессе',
  COMPLETED: 'Пройден',
}

const TEST_DESCRIPTIONS: Record<string, string> = {
  core: 'Комплексная оценка личности, мотивации и командных ролей',
  '64qn': 'Оценка личностных особенностей по 64 утверждениям',
}

function StatusBadge({status}: {status: SurveyStatus}) {
  const colors =
    status === 'COMPLETED'
      ? 'bg-emerald-100 text-emerald-800'
      : status === 'IN_PROGRESS'
        ? 'bg-sky-100 text-sky-800'
        : 'bg-stone-100 text-stone-600'
  return <span className={`rounded-full px-2.5 py-0.5 text-xs font-semibold ${colors}`}>{STATUS_LABELS[status]}</span>
}

function LockedBadge() {
  return (
    <span className="rounded-full bg-stone-100 px-2.5 py-0.5 text-xs font-semibold text-stone-500">
      🔒 Заблокирован
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
  if (data == null) {
    return (
      <div className="flex justify-center py-16">
        <Spinner />
      </div>
    )
  }

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="Тесты личности 📝"
        subtitle={`Пройдено ${data.testsCompleted} из ${data.testsTotal}`}
        backTo="/seeker/personality"
        backLabel="К личности"
      />

      <div className="flex flex-col gap-4">
        {data.groups.map((group) => {
          const canStart = !group.locked && group.status !== 'COMPLETED' && group.entrySurveyId != null
          return (
            <Card key={group.code}>
              <div className="flex flex-wrap items-center justify-between gap-4">
                <div>
                  <div className="flex items-center gap-2">
                    <h2 className="text-lg font-semibold text-stone-900">{group.name}</h2>
                    {group.locked ? <LockedBadge /> : <StatusBadge status={group.status} />}
                  </div>
                  <p className="mt-1 text-sm text-stone-500">
                    {TEST_DESCRIPTIONS[group.code] ?? ''}
                  </p>
                </div>
                {canStart && (
                  <Link to={`/seeker/personality/tests/${group.entrySurveyId}`}>
                    <Button size="sm">
                      {group.status === 'IN_PROGRESS' ? 'Продолжить' : 'Начать'}
                    </Button>
                  </Link>
                )}
              </div>
            </Card>
          )
        })}
      </div>
    </div>
  )
}
