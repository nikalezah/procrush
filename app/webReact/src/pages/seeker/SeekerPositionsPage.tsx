import { useEffect, useState } from 'react'
import {
  fetchDesiredPositions,
  fetchRecommendations,
  updateDesiredPositions,
} from '../../api/seekerApi'
import type { JobRecommendationDto } from '../../api/types'
import { FormSection } from '../../components/FormSection'
import { MatchScoreBadge } from '../../components/MatchScoreBadge'
import { OccupationPicker } from '../../components/OccupationPicker'

export function SeekerPositionsPage() {
  const [occupationIds, setOccupationIds] = useState<number[]>([])
  const [recommendations, setRecommendations] = useState<JobRecommendationDto[]>([])
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    void Promise.all([fetchDesiredPositions(), fetchRecommendations()])
      .then(([positions, recs]) => {
        setOccupationIds(positions.occupationIds)
        setRecommendations(recs)
      })
      .catch((e: Error) => setError(e.message))
  }, [])

  async function savePositions(ids: number[]) {
    setOccupationIds(ids)
    setMessage(null)
    setError(null)
    try {
      await updateDesiredPositions(ids)
      setMessage('Желаемые должности сохранены')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка сохранения')
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="text-2xl font-semibold">Должности и рекомендации</h1>
        <p className="mt-1 text-sm text-neutral-600">
          Выберите интересующие должности и просмотрите подборку вакансий
        </p>
      </div>
      {message != null && <p className="text-sm text-green-700">{message}</p>}
      {error != null && <p className="text-sm text-red-600">{error}</p>}

      <FormSection
        title="Желаемые должности"
        description="Выберите одну или несколько профессий"
      >
        <OccupationPicker
          selectedIds={occupationIds}
          onChange={(ids) => void savePositions(ids)}
        />
      </FormSection>

      <FormSection title="Рекомендованные вакансии" description="Демо-подборка (заглушка)">
        <div className="flex flex-col gap-3">
          {recommendations.map((job) => (
            <article
              key={job.id}
              className="flex flex-col gap-3 rounded-lg border border-neutral-200 p-4 sm:flex-row sm:items-start sm:justify-between"
            >
              <div>
                <h3 className="font-medium">{job.positionName}</h3>
                <p className="text-sm text-neutral-600">{job.companyName}</p>
                <p className="mt-2 text-sm text-neutral-700">{job.description}</p>
              </div>
              <div className="flex flex-col items-end gap-2">
                <MatchScoreBadge
                  score={job.matchScoreDisplay}
                  testsCompleted={job.testsCompleted}
                  isScoreReduced={job.isScoreReduced}
                />
                <button
                  type="button"
                  disabled
                  className="rounded-lg bg-neutral-200 px-3 py-1.5 text-sm text-neutral-500"
                >
                  Откликнуться (скоро)
                </button>
              </div>
            </article>
          ))}
        </div>
      </FormSection>
    </div>
  )
}
