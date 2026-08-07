package jobs.procrush.personality.observability

import jobs.procrush.personality.messaging.MessagingLog
import jobs.procrush.personality.messaging.PersonalityResultPublisher
import jobs.procrush.shared.CorrelationIds
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

class MessagingLogCorrelationTest {
    private val originalOut = System.out
    private val captured = ByteArrayOutputStream()

    @BeforeTest
    fun setUp() {
        WorkerObservability.initialize("personality-test")
        System.setOut(PrintStream(captured, true, Charsets.UTF_8))
    }

    @AfterTest
    fun tearDown() {
        System.setOut(originalOut)
    }

    @Test
    fun publisherAdapterLogsRetainRequestIdAcrossInfoBlockingBridge() {
        val logger = Logger.get(PersonalityResultPublisher::class)
        val messagingLog =
            MessagingLog { message, args ->
                Logger.infoBlocking(logger, message, *args)
            }

        Correlation.runWith(mapOf(CorrelationIds.REQUEST_ID to "req-delivery-42")) {
            messagingLog.info("Published personality generation result seekerId={} status={}", 7L, "OK")
        }

        val output = captured.toString(Charsets.UTF_8)
        assertTrue(
            output.contains("req-delivery-42"),
            "expected request id in publisher adapter log, got:\n$output",
        )
        assertTrue(
            output.contains("Published personality generation result"),
            "expected log message in output, got:\n$output",
        )
    }
}
