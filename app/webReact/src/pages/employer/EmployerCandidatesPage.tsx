import {Link, useParams} from 'react-router-dom'
import {useEffect, useState} from 'react'
import {fetchCandidatesOverview, fetchEmployerInterests, respondToCandidate,} from '../../api/employerApi'
import type {CandidateRecommendationDto, EmployerInterestsResponseDto} from '../../api/types'
import {ContactInfoPanel} from '../../components/ContactInfoPanel'
import {EmptyState} from '../../components/EmptyState'
import {FormSection} from '../../components/FormSection'
import {InterestStatusBadge} from '../../components/InterestStatusBadge'
import {MatchScoreBadge} from '../../components/MatchScoreBadge'
import {RespondButton} from '../../components/RespondButton'
import {Spinner} from '../../components/Spinner'

function CandidateRecommendationCard({
  candidate,
  respondingId,
  onRespond,
}: {
  candidate: CandidateRecommendationDto
  respondingId: number | null
  onRespond: (seekerId: number) => void
}) {
  return (
    <li className="flex flex-col gap-3 rounded-xl border border-neutral-200 bg-white p-4 sm:flex-row sm:items-start sm:justify-between">
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
      <div className="flex w-full flex-col items-end gap-2 sm:w-auto">
        <MatchScoreBadge score={candidate.matchScoreDisplay} />
        <InterestStatusBadge status={candidate.interestStatus} perspective="employer" />
        <RespondButton
          status={candidate.interestStatus}
          loading={respondingId === candidate.id}
          onRespond={() => onRespond(candidate.id)}
        />
        {candidate.interestStatus === 'MUTUAL' && candidate.contactInfo != null && (
          <ContactInfoPanel contactInfo={candidate.contactInfo} perspective="employer" />
        )}
      </div>
    </li>
  )
}

export function EmployerCandidatesPage() {
  const { id } = useParams<{ id: string }>()
  const profileId = Number(id)
  const [candidates, setCandidates] = useState<CandidateRecommendationDto[]>([])
  const [interests, setInterests] = useState<EmployerInterestsResponseDto>({
    respondedOutside: [],
    mutualOutside: [],
  })
  const [loading, setLoading] = useState(true)
  const [respondingId, setRespondingId] = useState<number | null>(null)
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  async function loadData(jobProfileId: number) {
    const overview = await fetchCandidatesOverview(jobProfileId)
    setCandidates(overview.candidates)
    setInterests(overview.interests)
  }

  useEffect(() => {
    if (Number.isNaN(profileId)) return
    setLoading(true)
    void loadData(profileId)
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false))
  }, [profileId])

  async function handleRespond(seekerId: number) {
    if (Number.isNaN(profileId)) return
    setError(null)
    setRespondingId(seekerId)
    try {
      const updated = await respondToCandidate(profileId, seekerId)
      setCandidates((prev) =>
        prev.map((candidate) => (candidate.id === seekerId ? updated : candidate)),
      )
      const outside = await fetchEmployerInterests(profileId)
      setInterests(outside)
      setMessage('Отклик отправлен')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Не удалось отправить отклик')
    } finally {
      setRespondingId(null)
    }
  }

  if (loading) {
    return (
      <div className="flex justify-center py-16">
        <Spinner />
      </div>
    )
  }

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
      {message != null && <p className="text-sm text-green-700">{message}</p>}
      {error != null && <p className="text-sm text-red-600">{error}</p>}

      {candidates.length === 0 ? (
        <EmptyState
          title="Кандидаты не найдены"
          description="Пока нет соискателей, прошедших оба теста и указавших эту должность"
        />
      ) : (
        <ul className="flex flex-col gap-3">
          {candidates.map((candidate) => (
            <CandidateRecommendationCard
              key={candidate.id}
              candidate={candidate}
              respondingId={respondingId}
              onRespond={(seekerId) => void handleRespond(seekerId)}
            />
          ))}
        </ul>
      )}

      {(interests.respondedOutside.length > 0 || interests.mutualOutside.length > 0) && (
        <>
          {interests.respondedOutside.length > 0 && (
            <FormSection
              title="Мои отклики вне рекомендаций"
              description="Кандидаты, на которых вы откликнулись, но они больше не в текущем подборе"
            >
              <ul className="flex flex-col gap-3">
                {interests.respondedOutside.map((candidate) => (
                  <CandidateRecommendationCard
                    key={`responded-${candidate.id}`}
                    candidate={candidate}
                    respondingId={respondingId}
                    onRespond={(seekerId) => void handleRespond(seekerId)}
                  />
                ))}
              </ul>
            </FormSection>
          )}

          {interests.mutualOutside.length > 0 && (
            <FormSection
              title="Взаимные отклики вне рекомендаций"
              description="Взаимный интерес с кандидатами вне текущего подбора"
            >
              <ul className="flex flex-col gap-3">
                {interests.mutualOutside.map((candidate) => (
                  <CandidateRecommendationCard
                    key={`mutual-${candidate.id}`}
                    candidate={candidate}
                    respondingId={respondingId}
                    onRespond={(seekerId) => void handleRespond(seekerId)}
                  />
                ))}
              </ul>
            </FormSection>
          )}
        </>
      )}
    </div>
  )
}
