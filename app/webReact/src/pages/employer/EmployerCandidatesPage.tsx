import {Link, useParams} from 'react-router-dom'
import {useEffect, useState} from 'react'
import {fetchCandidates} from '../../api/employerApi'
import type {CandidateRecommendationDto} from '../../api/types'
import {EmptyState} from '../../components/EmptyState'
import {MatchScoreBadge} from '../../components/MatchScoreBadge'

export function EmployerCandidatesPage() {
  const { id } = useParams<{ id: string }>()
  const profileId = Number(id)
  const [candidates, setCandidates] = useState<CandidateRecommendationDto[]>([])
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (Number.isNaN(profileId)) return
    void fetchCandidates(profileId)
      .then(setCandidates)
      .catch((e: Error) => setError(e.message))
  }, [profileId])

  return (
    <div className="flex flex-col gap-6">
      <div>
        <Link to="/employer/profiles" className="text-sm text-neutral-600 underline">
          ← К профилям
        </Link>
        <h1 className="mt-2 text-2xl font-semibold">Подобранные кандидаты</h1>
        <p className="mt-1 text-sm text-neutral-600">
          Кандидаты с пройденными тестами и совпадающей желаемой должностью
        </p>
      </div>
      {error != null && <p className="text-sm text-red-600">{error}</p>}
      {candidates.length === 0 ? (
        <EmptyState
          title="Кандидаты не найдены"
          description="Пока нет соискателей, прошедших оба теста и указавших эту должность"
        />
      ) : (
        <ul className="flex flex-col gap-3">
          {candidates.map((candidate) => (
            <li
              key={candidate.id}
              className="flex flex-col gap-3 rounded-xl border border-neutral-200 bg-white p-4 sm:flex-row sm:items-start sm:justify-between"
            >
              <div>
                <h3 className="font-medium">
                  {candidate.firstName} {candidate.lastName}
                </h3>
                <p className="text-sm text-neutral-600">{candidate.positionName}</p>
                <div className="mt-2 flex flex-wrap gap-1">
                  {candidate.skills.map((skill) => (
                    <span key={skill} className="rounded-full bg-neutral-100 px-2 py-0.5 text-xs">
                      {skill}
                    </span>
                  ))}
                </div>
              </div>
              <div className="flex flex-col items-end gap-2">
                <MatchScoreBadge score={candidate.matchScoreDisplay} />
                <button
                  type="button"
                  disabled
                  className="rounded-lg bg-neutral-200 px-3 py-1.5 text-sm text-neutral-500"
                >
                  Откликнуться (скоро)
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
