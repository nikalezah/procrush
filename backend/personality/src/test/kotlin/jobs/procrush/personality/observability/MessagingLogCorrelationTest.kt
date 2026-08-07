package jobs.procrush.personality.observability

import jobs.procrush.personality.messaging.MessagingLog
import jobs.procrush.personality.messaging.PersonalityResultPublisher
import jobs.procrush.shared.CorrelationIds
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
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

    @Test
    fun overlappingDeliveriesRetainOwnRequestIdsInAdapterLogs() {
        val logger = Logger.get(PersonalityResultPublisher::class)
        val messagingLog =
            MessagingLog { message, args ->
                Logger.infoBlocking(logger, message, *args)
            }

        val requestIds = listOf("req-overlap-A", "req-overlap-B")
        val ready = CyclicBarrier(requestIds.size)
        val errors = mutableListOf<Throwable>()
        val errorLock = Any()

        val workers =
            requestIds.map { reqId ->
                thread(name = "delivery-$reqId") {
                    try {
                        Correlation.runWith(mapOf(CorrelationIds.REQUEST_ID to reqId)) {
                            ready.await(5, TimeUnit.SECONDS)
                            messagingLog.info("adapter-log marker={}", reqId)
                            ready.await(5, TimeUnit.SECONDS)
                        }
                    } catch (error: Throwable) {
                        synchronized(errorLock) { errors += error }
                    }
                }
            }
        workers.forEach { it.join(10_000) }

        assertTrue(errors.isEmpty(), "delivery threads failed: $errors")
        val output = captured.toString(Charsets.UTF_8)
        val lines =
            output
                .lineSequence()
                .filter { it.contains("adapter-log marker=") }
                .toList()
        assertEquals(requestIds.size, lines.size, "expected one adapter log per delivery, got:\n$output")

        lines.forEach { line ->
            val marker =
                Regex("""adapter-log marker=(\S+)""")
                    .find(line)
                    ?.groupValues
                    ?.get(1)
            assertTrue(marker != null, "missing marker in line: $line")
            // TEXT format embeds request id as `[requestId]` before the message.
            assertTrue(
                line.contains("[$marker]"),
                "expected bracketed request id [$marker] on its own adapter log line, got:\n$line",
            )
        }
    }
}
