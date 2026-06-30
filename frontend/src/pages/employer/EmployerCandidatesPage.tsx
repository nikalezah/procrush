import {useParams} from 'react-router-dom'
import {useEffect, useRef, useState} from 'react'
import {useTranslation} from 'react-i18next'
import {fetchCandidatesOverview, fetchEmployerInterests, respondToCandidate} from '../../api/employerApi'
import type {CandidateRecommendationDto, EmployerInterestsResponseDto, MatchInterestEventDto} from '../../api/types'
import {ContactInfoPanel} from '../../components/ContactInfoPanel'
import {EmptyState} from '../../components/EmptyState'
import {FormSection} from '../../components/FormSection'
import {InterestStatusBadge} from '../../components/InterestStatusBadge'
import {MatchScoreBadge} from '../../components/MatchScoreBadge'
import {RespondButton} from '../../components/RespondButton'
import {Spinner} from '../../components/Spinner'
import {Alert} from '../../components/ui/Alert'
import {Avatar} from '../../components/ui/Avatar'
import {Card} from '../../components/ui/Card'
import {PageHeader} from '../../components/ui/PageHeader'
import {useMatchInterestEvents} from '../../hooks/useMatchInterestEvents'
import {resolveError} from '../../i18n/resolveApiError'

function patchEmployerCandidateFromEvent(
  candidate: CandidateRecommendationDto,
  event: MatchInterestEventDto,
): CandidateRecommendationDto {
  if (candidate.id !== event.seekerId) return candidate
  return {
    ...candidate,
    interestStatus: event.interestStatus,
    contactInfo: event.seekerContact,
  }
}

function CandidateRecommendationCard({
  candidate,
  respondingId,
  highlighted,
  onRespond,
}: {
  candidate: CandidateRecommendationDto
  respondingId: number | null
  highlighted?: boolean
  onRespond: (seekerId: number) => void
}) {
  const fullName = `${candidate.firstName} ${candidate.lastName}`
  const isMutual = candidate.interestStatus === 'MUTUAL'

  return (
    <Card
      highlighted={highlighted}
      className={isMutual ? 'border-accent-200 bg-surface-muted dark:border-accent-800 dark:bg-accent-950/30' : ''}
    >
      <div className="flex flex-col gap-4">
        <div className="flex items-start gap-4">
          <Avatar name={fullName} size="lg" />
          <div className="min-w-0 flex-1">
            <h3 className="text-lg font-semibold text-foreground">{fullName}</h3>
            <p className="text-sm text-muted">{candidate.positionName}</p>
            <div className="mt-3 flex flex-wrap gap-1.5">
              {candidate.skills.map((skill) => (
                <span
                  key={skill}
                  className="rounded-full bg-surface-muted px-2.5 py-0.5 text-xs font-medium text-brand-700 dark:text-brand-300"
                >
                  {skill}
                </span>
              ))}
            </div>
          </div>
          <MatchScoreBadge score={candidate.matchScoreDisplay} />
        </div>

        <div className="flex flex-wrap items-center justify-between gap-3 border-t border-border-subtle pt-4">
          <InterestStatusBadge status={candidate.interestStatus} perspective="employer" />
          <RespondButton
            status={candidate.interestStatus}
            loading={respondingId === candidate.id}
            onRespond={() => onRespond(candidate.id)}
          />
        </div>

        {isMutual && candidate.contactInfo != null && (
          <ContactInfoPanel contactInfo={candidate.contactInfo} perspective="employer" />
        )}
      </div>
    </Card>
  )
}

export function EmployerCandidatesPage() {
  const {t} = useTranslation()
  const {id} = useParams<{id: string}>()
  const profileId = Number(id)
  const {lastEvent, lastEventId} = useMatchInterestEvents()
  const [candidates, setCandidates] = useState<CandidateRecommendationDto[]>([])
  const [interests, setInterests] = useState<EmployerInterestsResponseDto>({
    respondedOutside: [],
    mutualOutside: [],
  })
  const [loading, setLoading] = useState(true)
  const [respondingId, setRespondingId] = useState<number | null>(null)
  const [highlightedId, setHighlightedId] = useState<number | null>(null)
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const lastHandledEventId = useRef(0)

  async function loadData(jobProfileId: number) {
    const overview = await fetchCandidatesOverview(jobProfileId)
    setCandidates(overview.candidates)
    setInterests(overview.interests)
  }

  useEffect(() => {
    if (Number.isNaN(profileId)) return
    setLoading(true)
    void loadData(profileId)
      .catch((err) => setError(resolveError(err)))
      .finally(() => setLoading(false))
    return () => setHighlightedId(null)
  }, [profileId])

  useEffect(() => {
    if (lastEvent == null || lastEventId === lastHandledEventId.current) return
    if (lastEvent.jobProfileId !== profileId) return
    lastHandledEventId.current = lastEventId

    setCandidates((prev) =>
      prev.map((candidate) => patchEmployerCandidateFromEvent(candidate, lastEvent)),
    )
    void fetchEmployerInterests(profileId).then(setInterests).catch(() => {})

    setHighlightedId(lastEvent.seekerId)
    const timer = window.setTimeout(() => setHighlightedId(null), 2500)
    return () => window.clearTimeout(timer)
  }, [lastEvent, lastEventId, profileId])

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
      setMessage(
        updated.interestStatus === 'MUTUAL'
          ? t('employer.candidates.mutualUnlocked')
          : t('employer.candidates.respondSent'),
      )
    } catch (err) {
      setError(resolveError(err) || t('common.respondError'))
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
      <PageHeader
        title={t('employer.candidates.title')}
        subtitle={t('employer.candidates.subtitle')}
        backTo="/employer/profiles"
        backLabel={t('employer.candidates.backLabel')}
      />

      {message != null && <Alert variant="success">{message}</Alert>}
      {error != null && <Alert variant="error">{error}</Alert>}

      {candidates.length === 0 ? (
        <EmptyState
          title={t('employer.candidates.emptyTitle')}
          description={t('employer.candidates.emptyDescription')}
          icon="🔍"
        />
      ) : (
        <ul className="flex flex-col gap-4">
          {candidates.map((candidate) => (
            <li key={candidate.id}>
              <CandidateRecommendationCard
                candidate={candidate}
                respondingId={respondingId}
                highlighted={highlightedId === candidate.id}
                onRespond={(seekerId) => void handleRespond(seekerId)}
              />
            </li>
          ))}
        </ul>
      )}

      {(interests.respondedOutside.length > 0 || interests.mutualOutside.length > 0) && (
        <>
          {interests.respondedOutside.length > 0 && (
            <FormSection
              title={t('employer.candidates.respondedOutside.title')}
              description={t('employer.candidates.respondedOutside.description')}
            >
              <ul className="flex flex-col gap-4">
                {interests.respondedOutside.map((candidate) => (
                  <li key={`responded-${candidate.id}`}>
                    <CandidateRecommendationCard
                      candidate={candidate}
                      respondingId={respondingId}
                      onRespond={(seekerId) => void handleRespond(seekerId)}
                    />
                  </li>
                ))}
              </ul>
            </FormSection>
          )}

          {interests.mutualOutside.length > 0 && (
            <FormSection
              title={t('employer.candidates.mutualOutside.title')}
              description={t('employer.candidates.mutualOutside.description')}
            >
              <ul className="flex flex-col gap-4">
                {interests.mutualOutside.map((candidate) => (
                  <li key={`mutual-${candidate.id}`}>
                    <CandidateRecommendationCard
                      candidate={candidate}
                      respondingId={respondingId}
                      onRespond={(seekerId) => void handleRespond(seekerId)}
                    />
                  </li>
                ))}
              </ul>
            </FormSection>
          )}
        </>
      )}
    </div>
  )
}
