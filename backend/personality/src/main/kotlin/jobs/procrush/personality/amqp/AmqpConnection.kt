package jobs.procrush.personality.amqp

import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.readByte
import io.ktor.utils.io.readByteArray
import io.ktor.utils.io.readInt
import io.ktor.utils.io.readShort
import io.ktor.utils.io.writeByte
import io.ktor.utils.io.writeByteArray
import io.ktor.utils.io.writeFully
import io.ktor.utils.io.writeInt
import io.ktor.utils.io.writeShort
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

internal class AmqpConnection private constructor(
    private val socket: Socket,
    private val input: ByteReadChannel,
    private val output: ByteWriteChannel,
    private val selectorManager: SelectorManager,
    private val scope: CoroutineScope,
    private val heartbeatSeconds: Int,
) {
    private val writeMutex = Mutex()
    private val channels = LinkedHashMap<Int, AmqpChannel>()
    private var nextChannelId = 1
    private var readerJob: Job? = null
    private var heartbeatJob: Job? = null
    private val connectionRpc = Mutex()
    private var pendingConnectionRpc: CompletableDeferred<MethodFrame>? = null
    @Volatile private var open = true
    private var lastReadMark = TimeSource.Monotonic.markNow()
    private var lastWriteMark = TimeSource.Monotonic.markNow()

    fun isOpen(): Boolean = open && socket.socketContext.isActive

    suspend fun openChannel(): AmqpChannel {
        check(isOpen()) { "AMQP connection is closed" }
        val channelId =
            writeMutex.withLock {
                val id = nextChannelId++
                val channel = AmqpChannel(this, id)
                channels[id] = channel
                channel
            }
        channelId.open()
        return channelId
    }

    suspend fun close() {
        if (!open) return
        open = false
        runCatching {
            rpcConnection(
                AmqpClass.CONNECTION,
                AmqpMethod.CLOSE,
                AmqpBuffer()
                    .writeShort(200)
                    .writeShortstr("OK")
                    .writeShort(0)
                    .writeShort(0)
                    .toByteArray(),
                expectClass = AmqpClass.CONNECTION,
                expectMethod = AmqpMethod.CLOSE_OK,
            )
        }
        heartbeatJob?.cancel()
        readerJob?.cancel()
        channels.values.forEach { it.markClosed() }
        channels.clear()
        runCatching { socket.close() }
        runCatching { selectorManager.close() }
        scope.cancel()
    }

    internal fun launch(block: suspend CoroutineScope.() -> Unit): Job = scope.launch(block = block)

    internal suspend fun writeMethod(
        channel: Int,
        classId: Int,
        methodId: Int,
        args: ByteArray = ByteArray(0),
    ) {
        val payload =
            AmqpBuffer()
                .writeShort(classId)
                .writeShort(methodId)
                .toByteArray() + args
        writeFrame(AmqpFrame.TYPE_METHOD, channel, payload)
    }

    internal suspend fun writeContent(
        channel: Int,
        classId: Int,
        body: ByteArray,
        properties: ByteArray,
    ) {
        val header =
            AmqpBuffer()
                .writeShort(classId)
                .writeShort(0) // weight
                .writeLongLong(body.size.toLong())
                .toByteArray() + properties
        writeFrame(AmqpFrame.TYPE_HEADER, channel, header)
        // Single body frame is fine for our message sizes.
        writeFrame(AmqpFrame.TYPE_BODY, channel, body)
    }

    internal suspend fun writeFrame(
        type: Byte,
        channel: Int,
        payload: ByteArray,
    ) {
        writeMutex.withLock {
            output.writeByte(type)
            output.writeShort(channel.toShort())
            output.writeInt(payload.size)
            if (payload.isNotEmpty()) {
                output.writeFully(payload, 0, payload.size)
            }
            output.writeByte(AmqpFrame.FRAME_END)
            output.flush()
            lastWriteMark = TimeSource.Monotonic.markNow()
        }
    }

    internal suspend fun rpc(
        channel: AmqpChannel,
        classId: Int,
        methodId: Int,
        args: ByteArray,
        expectClass: Int,
        expectMethod: Int,
    ): MethodFrame =
        withTimeout(30.seconds) {
            val deferred = CompletableDeferred<MethodFrame>()
            channel.setPendingRpc(expectClass, expectMethod, deferred)
            writeMethod(channel.channelId, classId, methodId, args)
            deferred.await()
        }

    private suspend fun rpcConnection(
        classId: Int,
        methodId: Int,
        args: ByteArray,
        expectClass: Int,
        expectMethod: Int,
    ): MethodFrame =
        connectionRpc.withLock {
            withTimeout(30.seconds) {
                val deferred = CompletableDeferred<MethodFrame>()
                pendingConnectionRpc = deferred
                writeMethod(0, classId, methodId, args)
                val frame = deferred.await()
                require(frame.classId == expectClass && frame.methodId == expectMethod) {
                    "Unexpected connection RPC response ${frame.classId}/${frame.methodId}"
                }
                frame
            }
        }

    private fun startLoops() {
        readerJob =
            scope.launch {
                try {
                    while (isActive && open) {
                        val frame = readFrame()
                        lastReadMark = TimeSource.Monotonic.markNow()
                        dispatchFrame(frame)
                    }
                } catch (_: Throwable) {
                    open = false
                    failPending(IllegalStateException("AMQP connection closed"))
                }
            }
        if (heartbeatSeconds > 0) {
            val intervalMs = (heartbeatSeconds * 1000L) / 2
            heartbeatJob =
                scope.launch {
                    while (isActive && open) {
                        delay(intervalMs)
                        val idleWrite = lastWriteMark.elapsedNow().inWholeMilliseconds
                        if (idleWrite >= intervalMs) {
                            runCatching {
                                writeFrame(AmqpFrame.TYPE_HEARTBEAT, 0, ByteArray(0))
                            }
                        }
                        val idleRead = lastReadMark.elapsedNow().inWholeMilliseconds
                        if (idleRead >= heartbeatSeconds * 1000L * 2) {
                            open = false
                            runCatching { socket.close() }
                            break
                        }
                    }
                }
        }
    }

    private suspend fun readFrame(): RawFrame {
        val type = input.readByte()
        val channel = input.readShort().toInt() and 0xffff
        val size = input.readInt()
        require(size >= 0) { "Negative AMQP frame size" }
        val payload = if (size == 0) ByteArray(0) else input.readByteArray(size)
        val end = input.readByte()
        require(end == AmqpFrame.FRAME_END) { "Invalid AMQP frame end: $end" }
        return RawFrame(type, channel, payload)
    }

    private suspend fun dispatchFrame(frame: RawFrame) {
        when (frame.type) {
            AmqpFrame.TYPE_HEARTBEAT -> Unit
            AmqpFrame.TYPE_METHOD -> dispatchMethod(frame.channel, frame.payload)
            AmqpFrame.TYPE_HEADER, AmqpFrame.TYPE_BODY -> {
                val ch = channels[frame.channel] ?: return
                ch.onContentFrame(frame)
            }
            else -> error("Unsupported AMQP frame type: ${frame.type}")
        }
    }

    private suspend fun dispatchMethod(
        channelId: Int,
        payload: ByteArray,
    ) {
        val reader = AmqpReader(payload)
        val classId = reader.readShort()
        val methodId = reader.readShort()
        val args = reader.readBytes(reader.remaining)
        val method = MethodFrame(classId, methodId, args)

        if (channelId == 0) {
            when {
                classId == AmqpClass.CONNECTION && methodId == AmqpMethod.CLOSE -> {
                    writeMethod(0, AmqpClass.CONNECTION, AmqpMethod.CLOSE_OK)
                    open = false
                    failPending(IllegalStateException("Broker closed AMQP connection"))
                }
                else -> pendingConnectionRpc?.complete(method)
            }
            return
        }

        val channel = channels[channelId] ?: return
        channel.onMethod(method)
    }

    private fun failPending(error: Throwable) {
        pendingConnectionRpc?.completeExceptionally(error)
        pendingConnectionRpc = null
        channels.values.forEach { it.failPending(error) }
    }

    companion object {
        suspend fun connect(url: AmqpUrl): AmqpConnection {
            val selectorManager = SelectorManager(Dispatchers.IO)
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val socket =
                aSocket(selectorManager).tcp().connect(url.host, url.port)
            val input = socket.openReadChannel()
            val output = socket.openWriteChannel(autoFlush = false)

            output.writeByteArray(AmqpFrame.PROTOCOL_HEADER)
            output.flush()

            // Connection.Start
            val start = readExpectedMethod(input, 0, AmqpClass.CONNECTION, AmqpMethod.START)
            AmqpReader(start.args).apply {
                readOctet() // version-major
                readOctet() // version-minor
                readTable()
                readLongstr() // mechanisms
                readLongstr() // locales
            }

            val auth =
                byteArrayOf(0) +
                    url.user.encodeToByteArray() +
                    byteArrayOf(0) +
                    url.password.encodeToByteArray()
            val startOkArgs =
                AmqpBuffer()
                    .writeEmptyTable()
                    .writeShortstr("PLAIN")
                    .writeLongstr(auth)
                    .writeShortstr("en_US")
                    .toByteArray()
            writeFrameStatic(output, AmqpFrame.TYPE_METHOD, 0, methodPayload(AmqpClass.CONNECTION, AmqpMethod.START_OK, startOkArgs))

            val tune = readExpectedMethod(input, 0, AmqpClass.CONNECTION, AmqpMethod.TUNE)
            val tuneReader = AmqpReader(tune.args)
            val channelMax = tuneReader.readShort()
            val frameMax = tuneReader.readLong()
            val heartbeat = tuneReader.readShort()
            val negotiatedHeartbeat = if (heartbeat == 0) 0 else heartbeat.coerceAtMost(60)

            val tuneOkArgs =
                AmqpBuffer()
                    .writeShort(if (channelMax == 0) 2047 else channelMax)
                    .writeLong(if (frameMax == 0) 131072 else frameMax)
                    .writeShort(negotiatedHeartbeat)
                    .toByteArray()
            writeFrameStatic(output, AmqpFrame.TYPE_METHOD, 0, methodPayload(AmqpClass.CONNECTION, AmqpMethod.TUNE_OK, tuneOkArgs))

            val openArgs =
                AmqpBuffer()
                    .writeShortstr(url.virtualHost)
                    .writeShortstr("")
                    .writeBits(false)
                    .toByteArray()
            writeFrameStatic(output, AmqpFrame.TYPE_METHOD, 0, methodPayload(AmqpClass.CONNECTION, AmqpMethod.OPEN, openArgs))
            readExpectedMethod(input, 0, AmqpClass.CONNECTION, AmqpMethod.OPEN_OK)

            val connection =
                AmqpConnection(
                    socket = socket,
                    input = input,
                    output = output,
                    selectorManager = selectorManager,
                    scope = scope,
                    heartbeatSeconds = negotiatedHeartbeat,
                )
            connection.startLoops()
            return connection
        }

        private fun methodPayload(
            classId: Int,
            methodId: Int,
            args: ByteArray,
        ): ByteArray =
            AmqpBuffer()
                .writeShort(classId)
                .writeShort(methodId)
                .toByteArray() + args

        private suspend fun writeFrameStatic(
            output: ByteWriteChannel,
            type: Byte,
            channel: Int,
            payload: ByteArray,
        ) {
            output.writeByte(type)
            output.writeShort(channel.toShort())
            output.writeInt(payload.size)
            if (payload.isNotEmpty()) {
                output.writeFully(payload, 0, payload.size)
            }
            output.writeByte(AmqpFrame.FRAME_END)
            output.flush()
        }

        private suspend fun readExpectedMethod(
            input: ByteReadChannel,
            expectedChannel: Int,
            expectedClass: Int,
            expectedMethod: Int,
        ): MethodFrame {
            while (true) {
                val type = input.readByte()
                val channel = input.readShort().toInt() and 0xffff
                val size = input.readInt()
                val payload = if (size == 0) ByteArray(0) else input.readByteArray(size)
                val end = input.readByte()
                require(end == AmqpFrame.FRAME_END) { "Invalid AMQP frame end" }
                if (type == AmqpFrame.TYPE_HEARTBEAT) continue
                require(type == AmqpFrame.TYPE_METHOD) { "Expected method frame, got $type" }
                require(channel == expectedChannel) { "Unexpected channel $channel" }
                val reader = AmqpReader(payload)
                val classId = reader.readShort()
                val methodId = reader.readShort()
                val args = reader.readBytes(reader.remaining)
                if (classId == AmqpClass.CONNECTION && methodId == AmqpMethod.CLOSE) {
                    val closeReader = AmqpReader(args)
                    val replyCode = closeReader.readShort()
                    val replyText = closeReader.readShortstr()
                    error("Broker closed connection during handshake: $replyCode $replyText")
                }
                require(classId == expectedClass && methodId == expectedMethod) {
                    "Expected $expectedClass/$expectedMethod, got $classId/$methodId"
                }
                return MethodFrame(classId, methodId, args)
            }
        }
    }
}

internal data class MethodFrame(
    val classId: Int,
    val methodId: Int,
    val args: ByteArray,
)

internal data class RawFrame(
    val type: Byte,
    val channel: Int,
    val payload: ByteArray,
)

internal data class AmqpDelivery(
    val consumerTag: String,
    val deliveryTag: Long,
    val redelivered: Boolean,
    val exchange: String,
    val routingKey: String,
    val properties: ContentProperties,
    val body: ByteArray,
)

internal data class ContentProperties(
    val contentType: String? = null,
    val deliveryMode: Int? = null,
    val headers: Map<String, String> = emptyMap(),
    val messageId: String? = null,
)
