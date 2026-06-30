import {useEffect, useRef, useState} from 'react'
import {useTranslation} from 'react-i18next'
import {fetchPositionsOverview, fetchSeekerInterests, respondToJob, updateDesiredPositions} from '../../api/seekerApi'
import type {
    JobRecommendationDto,
    MatchInterestEventDto,
    OccupationDto,
    SeekerInterestsResponseDto,
} from '../../api/types'
import {ContactInfoPanel} from '../../components/ContactInfoPanel'
import {EmptyState} from '../../components/EmptyState'
import {FormSection} from '../../components/FormSection'
import {InterestStatusBadge} from '../../components/InterestStatusBadge'
import {MatchScoreBadge} from '../../components/MatchScoreBadge'
import {OccupationPicker} from '../../components/OccupationPicker'
import {RespondButton} from '../../components/RespondButton'
import {Spinner} from '../../components/Spinner'
import {Alert} from '../../components/ui/Alert'
import {CompanyName, companyNameLabel} from '../../components/CompanyName'
import {Avatar} from '../../components/ui/Avatar'
import {Card} from '../../components/ui/Card'
import {PageHeader} from '../../components/ui/PageHeader'
import {useMatchInterestEvents} from '../../hooks/useMatchInterestEvents'
import {resolveError} from '../../i18n/resolveApiError'

function patchSeekerJobFromEvent(
  job: JobRecommendationDto,
  event: MatchInterestEventDto,
): JobRecommendationDto {
  if (job.id !== event.jobProfileId) return job
  return {
    ...job,
    interestStatus: event.interestStatus,
    contactInfo: event.employerContact,
  }
}

function JobRecommendationCard({
  job,
  respondingId,
  highlighted,
  onRespond,
}: {
  job: JobRecommendationDto
  respondingId: number | null
  highlighted?: boolean
  onRespond: (jobProfileId: number) => void
}) {
  const {t} = useTranslation()
  const isMutual = job.interestStatus === 'MUTUAL'

  return (
    <Card
      highlighted={highlighted}
      className={isMutual ? 'border-accent-200 bg-surface-muted dark:border-accent-800 dark:bg-accent-950/30' : ''}
    >
      <div className="flex flex-col gap-4">
        <div className="flex items-start gap-4">
          <Avatar name={companyNameLabel(job.companyName, t)} size="lg" />
          <div className="min-w-0 flex-1">
            <h3 className="text-lg font-semibold text-foreground">{job.positionName}</h3>
            <CompanyName name={job.companyName} className="text-sm text-muted" />
            {job.description != null && job.description !== '' && (
              <p className="mt-2 text-sm leading-relaxed text-muted">{job.description}</p>
            )}
          </div>
          <MatchScoreBadge score={job.matchScoreDisplay} />
        </div>

        <div className="flex flex-wrap items-center justify-between gap-3 border-t border-border-subtle pt-4">
          <InterestStatusBadge status={job.interestStatus} perspective="seeker" />
          <RespondButton
            status={job.interestStatus}
            loading={respondingId === job.id}
            onRespond={() => onRespond(job.id)}
          />
        </div>

        {isMutual && job.contactInfo != null && (
          <ContactInfoPanel contactInfo={job.contactInfo} perspective="seeker" />
        )}
      </div>
    </Card>
  )
}

export function SeekerPositionsPage() {
  const {t} = useTranslation()
  const {lastEvent, lastEventId} = useMatchInterestEvents()
  const [occupationIds, setOccupationIds] = useState<number[]>([])
  const [occupations, setOccupations] = useState<OccupationDto[]>([])
  const [recommendations, setRecommendations] = useState<JobRecommendationDto[]>([])
  const [interests, setInterests] = useState<SeekerInterestsResponseDto>({
    respondedOutside: [],
    mutualOutside: [],
  })
  const [testsComplete, setTestsComplete] = useState<boolean | null>(null)
  const [loading, setLoading] = useState(true)
  const [respondingId, setRespondingId] = useState<number | null>(null)
  const [highlightedId, setHighlightedId] = useState<number | null>(null)
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const lastHandledEventId = useRef(0)

  async function loadData() {
    const overview = await fetchPositionsOverview()
    setOccupationIds(overview.occupationIds)
    setOccupations(overview.occupations)
    setRecommendations(overview.recommendations)
    setInterests(overview.interests)
    setTestsComplete(overview.testsComplete)
  }

  useEffect(() => {
    setLoading(true)
    void loadData()
      .catch((err) => setError(resolveError(err)))
      .finally(() => setLoading(false))
    return () => setHighlightedId(null)
  }, [])

  useEffect(() => {
    if (lastEvent == null || lastEventId === lastHandledEventId.current) return
    lastHandledEventId.current = lastEventId

    setRecommendations((prev) => prev.map((job) => patchSeekerJobFromEvent(job, lastEvent)))
    void fetchSeekerInterests().then(setInterests).catch(() => {})

    setHighlightedId(lastEvent.jobProfileId)
    const timer = window.setTimeout(() => setHighlightedId(null), 2500)
    return () => window.clearTimeout(timer)
  }, [lastEvent, lastEventId])

  async function savePositions(ids: number[]) {
    setOccupationIds(ids)
    setMessage(null)
    setError(null)
    try {
      await updateDesiredPositions(ids)
      setLoading(true)
      await loadData()
      setMessage(t('seeker.positions.desiredPositionsSaved'))
    } catch (err) {
      setError(resolveError(err) || t('common.saveError'))
    } finally {
      setLoading(false)
    }
  }

  async function handleRespond(jobProfileId: number) {
    setError(null)
    setRespondingId(jobProfileId)
    try {
      const updated = await respondToJob(jobProfileId)
      setRecommendations((prev) => prev.map((job) => (job.id === jobProfileId ? updated : job)))
      const outside = await fetchSeekerInterests()
      setInterests(outside)
      setMessage(
        updated.interestStatus === 'MUTUAL'
          ? t('seeker.positions.mutualUnlocked')
          : t('seeker.positions.respondSent'),
      )
    } catch (err) {
      setError(resolveError(err) || t('common.respondError'))
    } finally {
      setRespondingId(null)
    }
  }

  function recommendationsEmptyMessage(): string {
    if (testsComplete === false) {
      return t('seeker.positions.recommendations.emptyTestsIncomplete')
    }
    if (occupationIds.length === 0) {
      return t('seeker.positions.recommendations.emptyNoPositions')
    }
    return t('seeker.positions.recommendations.emptyNoJobs')
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
      <PageHeader
        title={t('seeker.positions.title')}
        subtitle={t('seeker.positions.subtitle')}
      />

      {message != null && <Alert variant="success">{message}</Alert>}
      {error != null && <Alert variant="error">{error}</Alert>}

      <FormSection
        title={t('seeker.positions.whatLookingFor.title')}
        description={t('seeker.positions.whatLookingFor.description')}
      >
        <OccupationPicker
          selectedIds={occupationIds}
          occupations={occupations}
          onChange={(ids) => void savePositions(ids)}
        />
      </FormSection>

      <FormSection
        title={t('seeker.positions.recommendations.title')}
        description={t('seeker.positions.recommendations.description')}
      >
        {recommendations.length === 0 ? (
          <EmptyState
            title={t('seeker.positions.recommendations.emptyTitle')}
            description={recommendationsEmptyMessage()}
            icon="🔍"
          />
        ) : (
          <div className="flex flex-col gap-4">
            {recommendations.map((job) => (
              <JobRecommendationCard
                key={job.id}
                job={job}
                respondingId={respondingId}
                highlighted={highlightedId === job.id}
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
              title={t('seeker.positions.respondedOutside.title')}
              description={t('seeker.positions.respondedOutside.description')}
            >
              <div className="flex flex-col gap-4">
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
              title={t('seeker.positions.mutualOutside.title')}
              description={t('seeker.positions.mutualOutside.description')}
            >
              <div className="flex flex-col gap-4">
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
