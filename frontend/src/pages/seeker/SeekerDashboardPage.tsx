import {Link} from 'react-router-dom'
import {useEffect, useState} from 'react'
import {useTranslation} from 'react-i18next'
import {fetchSeekerDashboard} from '../../api/seekerApi'
import type {SeekerDashboardDto} from '../../api/types'
import {Alert} from '../../components/ui/Alert'
import {Button} from '../../components/ui/Button'
import {Card, StatCard} from '../../components/ui/Card'
import {MatchScoreBadge} from '../../components/MatchScoreBadge'
import {PageHeader} from '../../components/ui/PageHeader'
import {Spinner} from '../../components/Spinner'
import {EmptyState} from '../../components/EmptyState'
import {Avatar} from '../../components/ui/Avatar'
import {resolveError} from '../../i18n/resolveApiError'

export function SeekerDashboardPage() {
  const {t} = useTranslation()
  const [data, setData] = useState<SeekerDashboardDto | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    void fetchSeekerDashboard()
      .then(setData)
      .catch((err) => setError(resolveError(err)))
  }, [])

  if (error != null) {
    return <Alert variant="error">{error}</Alert>
  }
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
        title={t('seeker.dashboard.title')}
        subtitle={t('seeker.dashboard.subtitle')}
      />

      <Card className="flex items-center gap-4">
        <Avatar name={t('seeker.dashboard.avatarName')} size="lg" />
        <div className="flex-1">
          <p className="text-sm text-stone-500">{t('seeker.dashboard.profileCompletion')}</p>
          <div className="mt-2 flex items-center gap-3">
            <div className="h-2.5 flex-1 overflow-hidden rounded-full bg-brand-100">
              <div
                className="gradient-brand h-full rounded-full transition-all"
                style={{width: `${data.profileCompletionPercent}%`}}
              />
            </div>
            <span className="text-lg font-bold text-brand-600">{data.profileCompletionPercent}%</span>
          </div>
          <Link
            to="/seeker/profile"
            className="mt-2 inline-block text-sm font-medium text-brand-600 hover:text-brand-700"
          >
            {t('seeker.dashboard.improveProfile')}
          </Link>
        </div>
      </Card>

      <div className="grid gap-3 sm:grid-cols-2">
        <StatCard label={t('seeker.dashboard.stats.experience')} value={data.experienceCount} icon="💼" />
        <StatCard label={t('seeker.dashboard.stats.desiredPositions')} value={data.desiredPositionsCount} icon="🎯" />
      </div>

      {!data.testsComplete && (
        <Alert
          variant="warning"
          title={t('seeker.dashboard.testsAlert.title')}
          action={
            <Link to="/seeker/personality/tests">
              <Button size="sm">{t('seeker.dashboard.testsAlert.action')}</Button>
            </Link>
          }
        >
          {t('seeker.dashboard.testsAlert.body')}
        </Alert>
      )}

      <section>
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-stone-900">{t('seeker.dashboard.recentMatches.title')}</h2>
          <Link
            to="/seeker/positions"
            className="text-sm font-medium text-brand-600 hover:text-brand-700"
          >
            {t('common.viewAll')}
          </Link>
        </div>

        {data.recommendationsPreview.length === 0 ? (
          <EmptyState
            title={t('seeker.dashboard.recentMatches.emptyTitle')}
            description={
              data.testsComplete
                ? t('seeker.dashboard.recentMatches.emptyDescriptionTestsComplete')
                : t('seeker.dashboard.recentMatches.emptyDescriptionTestsIncomplete')
            }
            icon="💫"
            action={
              <Link to={data.testsComplete ? '/seeker/positions' : '/seeker/personality/tests'}>
                <Button size="sm">
                  {data.testsComplete
                    ? t('seeker.dashboard.recentMatches.selectPositions')
                    : t('seeker.dashboard.recentMatches.goToTests')}
                </Button>
              </Link>
            }
          />
        ) : (
          <div className="flex flex-col gap-3">
            {data.recommendationsPreview.map((job) => (
              <Card key={job.id} className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                <div className="flex items-start gap-3">
                  <Avatar name={job.companyName} size="md" />
                  <div>
                    <h3 className="font-semibold text-stone-900">{job.positionName}</h3>
                    <p className="text-sm text-stone-500">{job.companyName}</p>
                    {job.description != null && job.description !== '' && (
                      <p className="mt-2 line-clamp-2 text-sm text-stone-600">{job.description}</p>
                    )}
                  </div>
                </div>
                <MatchScoreBadge score={job.matchScoreDisplay} size="sm" />
              </Card>
            ))}
          </div>
        )}
      </section>
    </div>
  )
}
