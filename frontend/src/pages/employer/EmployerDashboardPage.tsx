import {Link} from 'react-router-dom'
import {useEffect, useState} from 'react'
import {fetchEmployerDashboard} from '../../api/employerApi'
import type {EmployerDashboardDto} from '../../api/types'

export function EmployerDashboardPage() {
  const [data, setData] = useState<EmployerDashboardDto | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    void fetchEmployerDashboard()
      .then(setData)
      .catch((e: Error) => setError(e.message))
  }, [])

  if (error != null) return <p className="text-sm text-red-600">{error}</p>
  if (data == null) return <p className="text-sm text-neutral-500">Загрузка…</p>

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="text-2xl font-semibold">Дашборд</h1>
        <p className="mt-1 text-sm text-neutral-600">{data.companyName}</p>
      </div>
      <div className="grid gap-4 sm:grid-cols-3">
        <div className="rounded-xl border border-neutral-200 bg-white p-4">
          <p className="text-sm text-neutral-600">Профили кандидатов</p>
          <p className="mt-1 text-2xl font-semibold">{data.jobProfilesCount}</p>
        </div>
        <div className="rounded-xl border border-neutral-200 bg-white p-4">
          <p className="text-sm text-neutral-600">Активные профили</p>
          <p className="mt-1 text-2xl font-semibold">{data.activeJobProfilesCount}</p>
        </div>
        <div className="rounded-xl border border-neutral-200 bg-white p-4">
          <p className="text-sm text-neutral-600">Подобрано кандидатов</p>
          <p className="mt-1 text-2xl font-semibold">{data.totalMatchedCandidates}</p>
        </div>
      </div>
      <Link
        to="/employer/profiles"
        className="self-start rounded-lg bg-neutral-900 px-4 py-2 text-sm font-medium text-white"
      >
        Управлять профилями
      </Link>
    </div>
  )
}
