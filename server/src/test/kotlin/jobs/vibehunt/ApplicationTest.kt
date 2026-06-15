package jobs.procrush

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import jobs.procrush.plugins.configureSerialization
import kotlin.test.*

class ApplicationTest {

    @Test
    fun testHealth() = testApplication {
        application {
            configureSerialization()
            routing {
                get("/health") {
                    call.respond(mapOf("status" to "ok"))
                }
            }
        }
        val response = client.get("/health")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("ok"))
    }
}
