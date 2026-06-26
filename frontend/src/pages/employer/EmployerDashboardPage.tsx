import {Link} from 'react-router-dom'
import {useEffect, useState} from 'react'
import {fetchEmployerDashboard} from '../../api/employerApi'
import type {EmployerDashboardDto} from '../../api/types'
import {Button} from '../../components/ui/Button'
import {StatCard} from '../../components/ui/Card'
import {PageHeader} from '../../components/ui/PageHeader'
import {Spinner} from '../../components/Spinner'

export function EmployerDashboardPage() {
  const [data, setData] = useState<EmployerDashboardDto | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    void fetchEmployerDashboard()
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
      <PageHeader title={`${data.companyName} 🏢`} subtitle="Обзор вакансий и подобранных кандидатов" />

      <div className="grid gap-3 sm:grid-cols-3">
        <StatCard label="Вакансии" value={data.jobProfilesCount} icon="💼" />
        <StatCard label="Активные" value={data.activeJobProfilesCount} icon="✅" />
        <StatCard label="Кандидаты" value={data.totalMatchedCandidates} icon="👥" />
      </div>

      <Link to="/employer/profiles">
        <Button fullWidth size="lg">
          Управлять вакансиями
        </Button>
      </Link>
    </div>
  )
}
