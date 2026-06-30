import {Link} from 'react-router-dom'
import {useEffect, useState} from 'react'
import {useTranslation} from 'react-i18next'
import {fetchEmployerDashboard} from '../../api/employerApi'
import type {EmployerDashboardDto} from '../../api/types'
import {Alert} from '../../components/ui/Alert'
import {Button} from '../../components/ui/Button'
import {StatCard} from '../../components/ui/Card'
import {PageHeader} from '../../components/ui/PageHeader'
import {Spinner} from '../../components/Spinner'
import {companyNameLabel} from '../../components/CompanyName'
import {resolveError} from '../../i18n/resolveApiError'

export function EmployerDashboardPage() {
  const {t} = useTranslation()
  const [data, setData] = useState<EmployerDashboardDto | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    void fetchEmployerDashboard()
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
      <PageHeader title={`${companyNameLabel(data.companyName, t)} 🏢`} subtitle={t('employer.dashboard.subtitle')} />

      <div className="grid gap-3 sm:grid-cols-3">
        <StatCard label={t('employer.dashboard.stats.profiles')} value={data.jobProfilesCount} icon="💼" />
        <StatCard label={t('employer.dashboard.stats.active')} value={data.activeJobProfilesCount} icon="✅" />
        <StatCard label={t('employer.dashboard.stats.candidates')} value={data.totalMatchedCandidates} icon="👥" />
      </div>

      <Link to="/employer/profiles">
        <Button fullWidth size="lg">
          {t('employer.dashboard.manageProfiles')}
        </Button>
      </Link>
    </div>
  )
}
