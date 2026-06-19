import {useEffect, useState} from 'react'
import {
  fetchDesiredPositions,
  fetchRecommendations,
  fetchSeekerInterests,
  fetchSurveyGroups,
  respondToJob,
  updateDesiredPositions,
} from '../../api/seekerApi'
import type {JobRecommendationDto, SeekerInterestsResponseDto} from '../../api/types'
import {ContactInfoPanel} from '../../components/ContactInfoPanel'
import {EmptyState} from '../../components/EmptyState'
import {FormSection} from '../../components/FormSection'
import {InterestStatusBadge} from '../../components/InterestStatusBadge'
import {MatchScoreBadge} from '../../components/MatchScoreBadge'
import {OccupationPicker} from '../../components/OccupationPicker'
import {RespondButton} from '../../components/RespondButton'

function JobRecommendationCard({
  job,
  respondingId,
  onRespond,
}: {
  job: JobRecommendationDto
  respondingId: number | null
  onRespond: (jobProfileId: number) => void
}) {
  return (
    <article className="flex flex-col gap-3 rounded-lg border border-neutral-200 p-4 sm:flex-row sm:items-start sm:justify-between">
      <div>
        <h3 className="font-medium">{job.positionName}</h3>
        <p className="text-sm text-neutral-600">{job.companyName}</p>
        <p className="mt-2 text-sm text-neutral-700">{job.description}</p>
      </div>
      <div className="flex w-full flex-col items-end gap-2 sm:w-auto">
        <MatchScoreBadge score={job.matchScoreDisplay} />
        <InterestStatusBadge status={job.interestStatus} perspective="seeker" />
        <RespondButton
          status={job.interestStatus}
          loading={respondingId === job.id}
          onRespond={() => onRespond(job.id)}
        />
        {job.interestStatus === 'MUTUAL' && job.contactInfo != null && (
          <ContactInfoPanel contactInfo={job.contactInfo} perspective="seeker" />
        )}
      </div>
    </article>
  )
}

export function SeekerPositionsPage() {
  const [occupationIds, setOccupationIds] = useState<number[]>([])
  const [recommendations, setRecommendations] = useState<JobRecommendationDto[]>([])
  const [interests, setInterests] = useState<SeekerInterestsResponseDto>({
    respondedOutside: [],
    mutualOutside: [],
  })
  const [testsComplete, setTestsComplete] = useState<boolean | null>(null)
  const [respondingId, setRespondingId] = useState<number | null>(null)
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  async function loadData() {
    const [positions, recs, outside, surveys] = await Promise.all([
      fetchDesiredPositions(),
      fetchRecommendations(),
      fetchSeekerInterests(),
      fetchSurveyGroups(),
    ])
    setOccupationIds(positions.occupationIds)
    setRecommendations(recs)
    setInterests(outside)
    setTestsComplete(surveys.testsCompleted >= surveys.testsTotal)
  }

  useEffect(() => {
    void loadData().catch((e: Error) => setError(e.message))
  }, [])

  async function savePositions(ids: number[]) {
    setOccupationIds(ids)
    setMessage(null)
    setError(null)
    try {
      await updateDesiredPositions(ids)
      await loadData()
      setMessage('Желаемые должности сохранены')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка сохранения')
    }
  }

  async function handleRespond(jobProfileId: number) {
    setError(null)
    setRespondingId(jobProfileId)
    try {
      const updated = await respondToJob(jobProfileId)
      setRecommendations((prev) =>
        prev.map((job) => (job.id === jobProfileId ? updated : job)),
      )
      const outside = await fetchSeekerInterests()
      setInterests(outside)
      setMessage('Отклик отправлен')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Не удалось отправить отклик')
    } finally {
      setRespondingId(null)
    }
  }

  function recommendationsEmptyMessage(): string {
    if (testsComplete === false) {
      return 'Пройдите оба теста личности, чтобы участвовать в подборе вакансий'
    }
    if (occupationIds.length === 0) {
      return 'Укажите одну или несколько желаемых должностей'
    }
    return 'Пока нет подходящих активных вакансий'
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

      <FormSection title="Рекомендованные вакансии" description="Подбор на основе навыков и личностного профиля">
        {recommendations.length === 0 ? (
          <EmptyState
            title="Рекомендаций пока нет"
            description={recommendationsEmptyMessage()}
          />
        ) : (
          <div className="flex flex-col gap-3">
            {recommendations.map((job) => (
              <JobRecommendationCard
                key={job.id}
                job={job}
                respondingId={respondingId}
                onRespond={(id) => void handleRespond(id)}
              />
            ))}
          </div>
        )}
      </FormSection>

      {(interests.respondedOutside.length > 0 || interests.mutualOutside.length > 0) && (
        <>
          {interests.respondedOutside.length > 0 && (
            <FormSection
              title="Мои отклики вне рекомендаций"
              description="Вакансии, на которые вы откликнулись, но они больше не в текущем подборе"
            >
              <div className="flex flex-col gap-3">
                {interests.respondedOutside.map((job) => (
                  <JobRecommendationCard
                    key={`responded-${job.id}`}
                    job={job}
                    respondingId={respondingId}
                    onRespond={(id) => void handleRespond(id)}
                  />
                ))}
              </div>
            </FormSection>
          )}

          {interests.mutualOutside.length > 0 && (
            <FormSection
              title="Взаимные отклики вне рекомендаций"
              description="Взаимный интерес с работодателями вне текущего подбора"
            >
              <div className="flex flex-col gap-3">
                {interests.mutualOutside.map((job) => (
                  <JobRecommendationCard
                    key={`mutual-${job.id}`}
                    job={job}
                    respondingId={respondingId}
                    onRespond={(id) => void handleRespond(id)}
                  />
                ))}
              </div>
            </FormSection>
          )}
        </>
      )}
    </div>
  )
}
