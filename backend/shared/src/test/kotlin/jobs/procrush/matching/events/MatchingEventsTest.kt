package jobs.procrush.matching.events

import jobs.procrush.personality.dto.PersonalityAxesDto
import kotlin.test.Test
import kotlin.test.assertEquals

class MatchingEventsTest {
    @Test
    fun envelopeRoundtripPreservesEventType() {
        val payload =
            SeekerProfileChangedPayload(
                seekerId = 7,
                desiredOccupationIds = listOf(1, 2),
                skillIds = listOf(3),
                personalityReady = true,
                personalityAxes = PersonalityAxesDto.DEFAULT,
                firstName = "Ivan",
                lastName = "Petrov",
                skillNames = listOf("Kotlin"),
            )
        val envelope =
            MatchingEventEnvelope(
                eventId = "evt-1",
                eventType = MatchingEventTypes.SEEKER_PROFILE_CHANGED,
                occurredAt = "2026-01-01T00:00:00Z",
                payload = MatchingEventJson.json.encodeToJsonElement(SeekerProfileChangedPayload.serializer(), payload),
            )
        val json = MatchingEventJson.json.encodeToString(MatchingEventEnvelope.serializer(), envelope)
        val decoded = MatchingEventJson.json.decodeFromString(MatchingEventEnvelope.serializer(), json)
        assertEquals(MatchingEventTypes.SEEKER_PROFILE_CHANGED, decoded.eventType)
        val decodedPayload = MatchingEventJson.decodePayload<SeekerProfileChangedPayload>(decoded)
        assertEquals(7, decodedPayload.seekerId)
    }
}
