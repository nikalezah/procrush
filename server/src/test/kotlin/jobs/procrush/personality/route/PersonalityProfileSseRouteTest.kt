package jobs.procrush.personality.route

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.application.install
import io.ktor.server.routing.routing
import io.ktor.server.sse.SSE
import io.ktor.server.sse.sse
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PersonalityProfileSseRouteTest {
    @Test
    fun personalityPreviewEvents_returnsTerminalStatusEvent() =
        testApplication {
            application {
                install(SSE)
                routing {
                    sse("/api/seeker/personality-preview/events") {
                        send(
                            data = """{"status":"READY"}""",
                            event = "personality-status",
                        )
                    }
                }
            }

            val response = client.get("/api/seeker/personality-preview/events")
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(ContentType.Text.EventStream, response.contentType()?.withoutParameters())
            val body = response.bodyAsText()
            assertTrue(body.contains("event: personality-status"))
            assertTrue(body.contains(""""status":"READY""""))
        }
}
