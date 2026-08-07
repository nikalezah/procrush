package jobs.procrush.personality.amqp

/**
 * AMQP 0-9-1 wire helpers (big-endian). Builds method payloads as byte arrays.
 */
internal class AmqpBuffer {
    private val bytes = ArrayList<Byte>(64)

    fun toByteArray(): ByteArray = bytes.toByteArray()

    fun writeOctet(value: Int): AmqpBuffer {
        bytes.add(value.toByte())
        return this
    }

    fun writeShort(value: Int): AmqpBuffer {
        bytes.add(((value ushr 8) and 0xff).toByte())
        bytes.add((value and 0xff).toByte())
        return this
    }

    fun writeLong(value: Int): AmqpBuffer {
        bytes.add(((value ushr 24) and 0xff).toByte())
        bytes.add(((value ushr 16) and 0xff).toByte())
        bytes.add(((value ushr 8) and 0xff).toByte())
        bytes.add((value and 0xff).toByte())
        return this
    }

    fun writeLongLong(value: Long): AmqpBuffer {
        for (shift in 56 downTo 0 step 8) {
            bytes.add(((value ushr shift) and 0xffL).toByte())
        }
        return this
    }

    fun writeShortstr(value: String): AmqpBuffer {
        val encoded = value.encodeToByteArray()
        require(encoded.size <= 255) { "shortstr too long: ${encoded.size}" }
        writeOctet(encoded.size)
        encoded.forEach { bytes.add(it) }
        return this
    }

    fun writeLongstr(value: ByteArray): AmqpBuffer {
        writeLong(value.size)
        value.forEach { bytes.add(it) }
        return this
    }

    fun writeLongstr(value: String): AmqpBuffer = writeLongstr(value.encodeToByteArray())

    fun writeBits(vararg bits: Boolean): AmqpBuffer {
        var packed = 0
        bits.forEachIndexed { index, bit ->
            if (bit) packed = packed or (1 shl index)
        }
        return writeOctet(packed)
    }

    fun writeTable(entries: Map<String, AmqpField>): AmqpBuffer {
        val table = AmqpBuffer()
        for ((key, field) in entries) {
            table.writeShortstr(key)
            field.writeTo(table)
        }
        val payload = table.toByteArray()
        writeLong(payload.size)
        payload.forEach { bytes.add(it) }
        return this
    }

    fun writeEmptyTable(): AmqpBuffer = writeLong(0)
}

internal sealed class AmqpField {
    abstract fun writeTo(buf: AmqpBuffer)

    data class LongString(val value: String) : AmqpField() {
        override fun writeTo(buf: AmqpBuffer) {
            buf.writeOctet('S'.code)
            buf.writeLongstr(value)
        }
    }
}

internal class AmqpReader(
    private val data: ByteArray,
) {
    private var pos = 0

    val remaining: Int get() = data.size - pos

    fun readOctet(): Int {
        require(pos < data.size)
        return data[pos++].toInt() and 0xff
    }

    fun readShort(): Int {
        val hi = readOctet()
        val lo = readOctet()
        return (hi shl 8) or lo
    }

    fun readLong(): Int {
        val b0 = readOctet()
        val b1 = readOctet()
        val b2 = readOctet()
        val b3 = readOctet()
        return (b0 shl 24) or (b1 shl 16) or (b2 shl 8) or b3
    }

    fun readLongLong(): Long {
        var value = 0L
        repeat(8) {
            value = (value shl 8) or readOctet().toLong()
        }
        return value
    }

    fun readShortstr(): String {
        val len = readOctet()
        return readBytes(len).decodeToString()
    }

    fun readLongstr(): ByteArray {
        val len = readLong()
        require(len >= 0)
        return readBytes(len)
    }

    fun readBytes(len: Int): ByteArray {
        require(len >= 0 && pos + len <= data.size)
        val out = data.copyOfRange(pos, pos + len)
        pos += len
        return out
    }

    fun skip(len: Int) {
        require(len >= 0 && pos + len <= data.size)
        pos += len
    }

    fun readTable() {
        val size = readLong()
        require(size >= 0)
        val end = pos + size
        while (pos < end) {
            readShortstr()
            skipFieldValue()
        }
        require(pos == end)
    }

    fun readBits(count: Int): BooleanArray {
        val packed = readOctet()
        return BooleanArray(count) { index -> (packed and (1 shl index)) != 0 }
    }

    private fun skipFieldValue() {
        when (val type = readOctet().toChar()) {
            't', 'b', 'B' -> skip(1)
            'U', 'u', 's' -> {
                if (type == 's') {
                    val len = readOctet()
                    skip(len)
                } else {
                    skip(2)
                }
            }
            'I', 'i', 'f' -> skip(4)
            'L', 'l', 'd', 'T' -> skip(8)
            'S', 'x' -> {
                val len = readLong()
                skip(len)
            }
            'D' -> skip(5)
            'V' -> Unit
            'F' -> readTable()
            'A' -> {
                val size = readLong()
                skip(size)
            }
            else -> error("Unsupported AMQP field type: $type")
        }
    }
}

internal object AmqpFrame {
    const val TYPE_METHOD: Byte = 1
    const val TYPE_HEADER: Byte = 2
    const val TYPE_BODY: Byte = 3
    const val TYPE_HEARTBEAT: Byte = 8
    const val FRAME_END: Byte = 0xCE.toByte()

    /** Wire overhead: type(1) + channel(2) + size(4) + frame-end(1). */
    const val EMPTY_FRAME_SIZE: Int = 8

    val PROTOCOL_HEADER: ByteArray =
        byteArrayOf(
            'A'.code.toByte(),
            'M'.code.toByte(),
            'Q'.code.toByte(),
            'P'.code.toByte(),
            0,
            0,
            9,
            1,
        )

    /**
     * Max body-frame payload octets for a negotiated [frameMax]
     * (total frame size including header and end byte).
     * [frameMax] 0 means unlimited.
     */
    fun maxBodyPayloadSize(frameMax: Int): Int {
        require(frameMax == 0 || frameMax > EMPTY_FRAME_SIZE) {
            "frameMax too small for an AMQP frame: $frameMax"
        }
        return if (frameMax == 0) Int.MAX_VALUE else frameMax - EMPTY_FRAME_SIZE
    }

    /** Split [body] into body-frame payloads that each respect [frameMax]. */
    fun bodyFramePayloads(
        body: ByteArray,
        frameMax: Int,
    ): List<ByteArray> {
        val maxPayload = maxBodyPayloadSize(frameMax)
        if (body.isEmpty()) return listOf(ByteArray(0))
        if (body.size <= maxPayload) return listOf(body)
        val chunks = ArrayList<ByteArray>((body.size + maxPayload - 1) / maxPayload)
        var offset = 0
        while (offset < body.size) {
            val end = minOf(offset + maxPayload, body.size)
            chunks.add(body.copyOfRange(offset, end))
            offset = end
        }
        return chunks
    }
}

internal object AmqpClass {
    const val CONNECTION = 10
    const val CHANNEL = 20
    const val EXCHANGE = 40
    const val QUEUE = 50
    const val BASIC = 60
}

internal object AmqpMethod {
    // connection
    const val START = 10
    const val START_OK = 11
    const val TUNE = 30
    const val TUNE_OK = 31
    const val OPEN = 40
    const val OPEN_OK = 41
    const val CLOSE = 50
    const val CLOSE_OK = 51

    // channel
    const val CHANNEL_OPEN = 10
    const val CHANNEL_OPEN_OK = 11
    const val CHANNEL_CLOSE = 40
    const val CHANNEL_CLOSE_OK = 41

    // exchange
    const val EXCHANGE_DECLARE = 10
    const val EXCHANGE_DECLARE_OK = 11

    // queue
    const val QUEUE_DECLARE = 10
    const val QUEUE_DECLARE_OK = 11
    const val QUEUE_BIND = 20
    const val QUEUE_BIND_OK = 21

    // basic
    const val QOS = 10
    const val QOS_OK = 11
    const val CONSUME = 20
    const val CONSUME_OK = 21
    const val CANCEL = 30
    const val CANCEL_OK = 31
    const val PUBLISH = 40
    const val DELIVER = 60
    const val ACK = 80
    const val NACK = 120
}
