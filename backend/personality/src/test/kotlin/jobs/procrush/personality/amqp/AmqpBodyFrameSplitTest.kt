package jobs.procrush.personality.amqp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AmqpBodyFrameSplitTest {
    @Test
    fun maxBodyPayloadSize_subtractsFrameOverhead() {
        assertEquals(131064, AmqpFrame.maxBodyPayloadSize(131072))
        assertEquals(Int.MAX_VALUE, AmqpFrame.maxBodyPayloadSize(0))
    }

    @Test
    fun bodyFramePayloads_keepsSmallBodyAsSingleFrame() {
        val body = ByteArray(100) { it.toByte() }
        val chunks = AmqpFrame.bodyFramePayloads(body, frameMax = 131072)
        assertEquals(1, chunks.size)
        assertTrue(chunks[0].contentEquals(body))
    }

    @Test
    fun bodyFramePayloads_splitsExactlyAtPayloadLimit() {
        val frameMax = 131072
        val maxPayload = AmqpFrame.maxBodyPayloadSize(frameMax)
        val body = ByteArray(maxPayload) { 1 }
        val chunks = AmqpFrame.bodyFramePayloads(body, frameMax)
        assertEquals(1, chunks.size)
        assertEquals(maxPayload, chunks[0].size)
        assertTrue(chunks[0].size + AmqpFrame.EMPTY_FRAME_SIZE <= frameMax)
    }

    @Test
    fun bodyFramePayloads_splitsOneByteOverPayloadLimit() {
        val frameMax = 131072
        val maxPayload = AmqpFrame.maxBodyPayloadSize(frameMax)
        val body = ByteArray(maxPayload + 1) { 2 }
        val chunks = AmqpFrame.bodyFramePayloads(body, frameMax)
        assertEquals(2, chunks.size)
        assertEquals(maxPayload, chunks[0].size)
        assertEquals(1, chunks[1].size)
        chunks.forEach { chunk ->
            assertTrue(chunk.size + AmqpFrame.EMPTY_FRAME_SIZE <= frameMax)
        }
        assertTrue(chunks[0].plus(chunks[1]).contentEquals(body))
    }

    @Test
    fun bodyFramePayloads_splits140kBodyThatPreviouslyFailedAgainstRabbitMq() {
        val frameMax = 131072
        val body = ByteArray(140_000) { (it % 251).toByte() }
        val chunks = AmqpFrame.bodyFramePayloads(body, frameMax)
        assertTrue(chunks.size >= 2)
        chunks.forEach { chunk ->
            assertTrue(
                chunk.size + AmqpFrame.EMPTY_FRAME_SIZE <= frameMax,
                "chunk ${chunk.size} exceeds frameMax=$frameMax",
            )
        }
        assertEquals(body.size, chunks.sumOf { it.size })
        assertTrue(chunks.reduce { acc, bytes -> acc + bytes }.contentEquals(body))
    }

    @Test
    fun bodyFramePayloads_emptyBodyStillEmitsOneEmptyChunk() {
        val chunks = AmqpFrame.bodyFramePayloads(ByteArray(0), frameMax = 131072)
        assertEquals(1, chunks.size)
        assertEquals(0, chunks[0].size)
    }
}
