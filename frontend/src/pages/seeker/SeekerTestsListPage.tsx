import {Link} from 'react-router-dom'
import {useEffect, useState} from 'react'
import {useTranslation} from 'react-i18next'
import {fetchSurveyGroups} from '../../api/seekerApi'
import type {SurveyGroupsResponseDto, SurveyStatus} from '../../api/types'
import {Alert} from '../../components/ui/Alert'
import {Button} from '../../components/ui/Button'
import {Card} from '../../components/ui/Card'
import {PageHeader} from '../../components/ui/PageHeader'
import {Spinner} from '../../components/Spinner'
import {resolveError} from '../../i18n/resolveApiError'

function StatusBadge({status}: {status: SurveyStatus}) {
  const {t} = useTranslation()
  const colors =
    status === 'COMPLETED'
      ? 'bg-emerald-100 text-emerald-800'
      : status === 'IN_PROGRESS'
        ? 'bg-sky-100 text-sky-800'
        : 'bg-surface-muted text-muted'
  const labelKey = `seeker.tests.status.${status === 'NOT_STARTED' ? 'notStarted' : status === 'IN_PROGRESS' ? 'inProgress' : 'completed'}` as const
  return <span className={`rounded-full px-2.5 py-0.5 text-xs font-semibold ${colors}`}>{t(labelKey)}</span>
}

function LockedBadge() {
  const {t} = useTranslation()
  return (
    <span className="rounded-full bg-surface-muted px-2.5 py-0.5 text-xs font-semibold text-muted">
      {t('seeker.tests.status.locked')}
    </span>
  )
}

export function SeekerTestsListPage() {
  const {t} = useTranslation()
  const [data, setData] = useState<SurveyGroupsResponseDto | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    void fetchSurveyGroups()
      .then(setData)
      .catch((err) => setError(resolveError(err)))
  }, [])

  if (error != null) return <Alert variant="error">{error}</Alert>
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
        title={t('seeker.tests.title')}
        subtitle={t('seeker.tests.subtitle', {completed: data.testsCompleted, total: data.testsTotal})}
        backTo="/seeker/personality"
        backLabel={t('seeker.tests.backLabel')}
      />

      <div className="flex flex-col gap-4">
        {data.groups.map((group) => {
          const canStart = !group.locked && group.status !== 'COMPLETED' && group.entrySurveyId != null
          const descriptionKey = `seeker.tests.descriptions.${group.code}` as const
          return (
            <Card key={group.code}>
              <div className="flex flex-wrap items-center justify-between gap-4">
                <div>
                  <div className="flex items-center gap-2">
                    <h2 className="text-lg font-semibold text-foreground">
                      {t(`seeker.testTake.titles.${group.code}` as const)}
                    </h2>
                    {group.locked ? <LockedBadge /> : <StatusBadge status={group.status} />}
                  </div>
                  <p className="mt-1 text-sm text-muted">
                    {t(descriptionKey, {defaultValue: ''})}
                  </p>
                </div>
                {canStart && (
                  <Link to={`/seeker/personality/tests/${group.entrySurveyId}`}>
                    <Button size="sm">
                      {group.status === 'IN_PROGRESS' ? t('seeker.tests.continue') : t('seeker.tests.start')}
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
