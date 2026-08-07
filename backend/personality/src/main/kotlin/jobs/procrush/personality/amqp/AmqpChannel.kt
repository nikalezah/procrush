package jobs.procrush.personality.amqp

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class AmqpChannel(
    private val connection: AmqpConnection,
    val channelId: Int,
) {
    private val rpcMutex = Mutex()
    private var pendingRpc: PendingRpc? = null
    @Volatile private var closed = false

    private var contentAssembler: ContentAssembler? = null
    private var deliveryHandler: (suspend (AmqpDelivery) -> Unit)? = null
    private var consumerTag: String? = null

    suspend fun open() {
        rpc(
            AmqpClass.CHANNEL,
            AmqpMethod.CHANNEL_OPEN,
            AmqpBuffer().writeShortstr("").toByteArray(),
            AmqpClass.CHANNEL,
            AmqpMethod.CHANNEL_OPEN_OK,
        )
    }

    suspend fun close() {
        if (closed) return
        runCatching {
            rpc(
                AmqpClass.CHANNEL,
                AmqpMethod.CHANNEL_CLOSE,
                AmqpBuffer()
                    .writeShort(200)
                    .writeShortstr("OK")
                    .writeShort(0)
                    .writeShort(0)
                    .toByteArray(),
                AmqpClass.CHANNEL,
                AmqpMethod.CHANNEL_CLOSE_OK,
            )
        }
        markClosed()
    }

    fun markClosed() {
        closed = true
        failPending(IllegalStateException("AMQP channel closed"))
    }

    suspend fun exchangeDeclare(
        exchange: String,
        type: String,
        durable: Boolean,
    ) {
        val args =
            AmqpBuffer()
                .writeShort(0)
                .writeShortstr(exchange)
                .writeShortstr(type)
                .writeBits(
                    false, // passive
                    durable,
                    false, // auto-delete
                    false, // internal
                    false, // no-wait
                )
                .writeEmptyTable()
                .toByteArray()
        rpc(AmqpClass.EXCHANGE, AmqpMethod.EXCHANGE_DECLARE, args, AmqpClass.EXCHANGE, AmqpMethod.EXCHANGE_DECLARE_OK)
    }

    suspend fun queueDeclare(
        queue: String,
        durable: Boolean,
        exclusive: Boolean = false,
        autoDelete: Boolean = false,
        arguments: Map<String, AmqpField> = emptyMap(),
    ) {
        val buf =
            AmqpBuffer()
                .writeShort(0)
                .writeShortstr(queue)
                .writeBits(
                    false, // passive
                    durable,
                    exclusive,
                    autoDelete,
                    false, // no-wait
                )
        if (arguments.isEmpty()) buf.writeEmptyTable() else buf.writeTable(arguments)
        rpc(AmqpClass.QUEUE, AmqpMethod.QUEUE_DECLARE, buf.toByteArray(), AmqpClass.QUEUE, AmqpMethod.QUEUE_DECLARE_OK)
    }

    suspend fun queueBind(
        queue: String,
        exchange: String,
        routingKey: String,
    ) {
        val args =
            AmqpBuffer()
                .writeShort(0)
                .writeShortstr(queue)
                .writeShortstr(exchange)
                .writeShortstr(routingKey)
                .writeBits(false) // no-wait
                .writeEmptyTable()
                .toByteArray()
        rpc(AmqpClass.QUEUE, AmqpMethod.QUEUE_BIND, args, AmqpClass.QUEUE, AmqpMethod.QUEUE_BIND_OK)
    }

    suspend fun basicQos(prefetchCount: Int) {
        val args =
            AmqpBuffer()
                .writeLong(0) // prefetch-size
                .writeShort(prefetchCount)
                .writeBits(false) // global
                .toByteArray()
        rpc(AmqpClass.BASIC, AmqpMethod.QOS, args, AmqpClass.BASIC, AmqpMethod.QOS_OK)
    }

    suspend fun basicPublish(
        exchange: String,
        routingKey: String,
        properties: ContentProperties,
        body: ByteArray,
    ) {
        val methodArgs =
            AmqpBuffer()
                .writeShort(0)
                .writeShortstr(exchange)
                .writeShortstr(routingKey)
                .writeBits(false, false) // mandatory, immediate
                .toByteArray()
        connection.writeMethod(channelId, AmqpClass.BASIC, AmqpMethod.PUBLISH, methodArgs)
        connection.writeContent(channelId, AmqpClass.BASIC, body, encodeProperties(properties))
    }

    suspend fun basicConsume(
        queue: String,
        handler: suspend (AmqpDelivery) -> Unit,
    ): String {
        deliveryHandler = handler
        val args =
            AmqpBuffer()
                .writeShort(0)
                .writeShortstr(queue)
                .writeShortstr("") // consumer-tag
                .writeBits(
                    false, // no-local
                    false, // no-ack (manual)
                    false, // exclusive
                    false, // no-wait
                )
                .writeEmptyTable()
                .toByteArray()
        val ok = rpc(AmqpClass.BASIC, AmqpMethod.CONSUME, args, AmqpClass.BASIC, AmqpMethod.CONSUME_OK)
        val tag = AmqpReader(ok.args).readShortstr()
        consumerTag = tag
        return tag
    }

    suspend fun basicCancel(tag: String) {
        val args =
            AmqpBuffer()
                .writeShortstr(tag)
                .writeBits(false)
                .toByteArray()
        rpc(AmqpClass.BASIC, AmqpMethod.CANCEL, args, AmqpClass.BASIC, AmqpMethod.CANCEL_OK)
        if (consumerTag == tag) {
            consumerTag = null
            deliveryHandler = null
        }
    }

    suspend fun basicAck(deliveryTag: Long) {
        val args =
            AmqpBuffer()
                .writeLongLong(deliveryTag)
                .writeBits(false)
                .toByteArray()
        connection.writeMethod(channelId, AmqpClass.BASIC, AmqpMethod.ACK, args)
    }

    suspend fun basicNack(
        deliveryTag: Long,
        requeue: Boolean,
    ) {
        val args =
            AmqpBuffer()
                .writeLongLong(deliveryTag)
                .writeBits(false, requeue) // multiple, requeue
                .toByteArray()
        connection.writeMethod(channelId, AmqpClass.BASIC, AmqpMethod.NACK, args)
    }

    fun getConsumerTag(): String? = consumerTag

    internal fun setPendingRpc(
        expectClass: Int,
        expectMethod: Int,
        deferred: CompletableDeferred<MethodFrame>,
    ) {
        pendingRpc = PendingRpc(expectClass, expectMethod, deferred)
    }

    internal fun failPending(error: Throwable) {
        pendingRpc?.deferred?.completeExceptionally(error)
        pendingRpc = null
    }

    internal suspend fun onMethod(method: MethodFrame) {
        when {
            method.classId == AmqpClass.BASIC && method.methodId == AmqpMethod.DELIVER -> {
                val reader = AmqpReader(method.args)
                val tag = reader.readShortstr()
                val deliveryTag = reader.readLongLong()
                val redelivered = reader.readBits(1)[0]
                val exchange = reader.readShortstr()
                val routingKey = reader.readShortstr()
                contentAssembler =
                    ContentAssembler.Delivery(
                        consumerTag = tag,
                        deliveryTag = deliveryTag,
                        redelivered = redelivered,
                        exchange = exchange,
                        routingKey = routingKey,
                    )
            }
            method.classId == AmqpClass.CHANNEL && method.methodId == AmqpMethod.CHANNEL_CLOSE -> {
                connection.writeMethod(channelId, AmqpClass.CHANNEL, AmqpMethod.CHANNEL_CLOSE_OK)
                markClosed()
            }
            else -> {
                val pending = pendingRpc
                if (pending != null &&
                    pending.expectClass == method.classId &&
                    pending.expectMethod == method.methodId
                ) {
                    pendingRpc = null
                    pending.deferred.complete(method)
                }
            }
        }
    }

    internal suspend fun onContentFrame(frame: RawFrame) {
        val assembler = contentAssembler ?: return
        when (frame.type) {
            AmqpFrame.TYPE_HEADER -> {
                val reader = AmqpReader(frame.payload)
                reader.readShort() // class-id
                reader.readShort() // weight
                val bodySize = reader.readLongLong()
                val properties = decodeProperties(reader)
                assembler.expectedBodySize = bodySize
                assembler.properties = properties
                assembler.body.clear()
                if (bodySize == 0L) {
                    finishDelivery(assembler)
                }
            }
            AmqpFrame.TYPE_BODY -> {
                assembler.body.addAll(frame.payload.toList())
                if (assembler.body.size.toLong() >= assembler.expectedBodySize) {
                    finishDelivery(assembler)
                }
            }
        }
    }

    private fun finishDelivery(assembler: ContentAssembler) {
        contentAssembler = null
        if (assembler !is ContentAssembler.Delivery) return
        val handler = deliveryHandler ?: return
        val delivery =
            AmqpDelivery(
                consumerTag = assembler.consumerTag,
                deliveryTag = assembler.deliveryTag,
                redelivered = assembler.redelivered,
                exchange = assembler.exchange,
                routingKey = assembler.routingKey,
                properties = assembler.properties,
                body = assembler.body.toByteArray(),
            )
        // Keep the connection reader free for heartbeats/RPC while user work runs.
        connection.launch {
            handler(delivery)
        }
    }

    private suspend fun rpc(
        classId: Int,
        methodId: Int,
        args: ByteArray,
        expectClass: Int,
        expectMethod: Int,
    ): MethodFrame =
        rpcMutex.withLock {
            connection.rpc(this, classId, methodId, args, expectClass, expectMethod)
        }

    private data class PendingRpc(
        val expectClass: Int,
        val expectMethod: Int,
        val deferred: CompletableDeferred<MethodFrame>,
    )

    private sealed class ContentAssembler {
        var expectedBodySize: Long = 0
        var properties: ContentProperties = ContentProperties()
        val body = ArrayList<Byte>()

        class Delivery(
            val consumerTag: String,
            val deliveryTag: Long,
            val redelivered: Boolean,
            val exchange: String,
            val routingKey: String,
        ) : ContentAssembler()
    }
}

internal fun encodeProperties(properties: ContentProperties): ByteArray {
    var flags = 0
    val body = AmqpBuffer()
    if (properties.contentType != null) {
        flags = flags or (1 shl 15)
        body.writeShortstr(properties.contentType)
    }
    if (properties.headers.isNotEmpty()) {
        flags = flags or (1 shl 13)
        body.writeTable(properties.headers.mapValues { AmqpField.LongString(it.value) })
    }
    if (properties.deliveryMode != null) {
        flags = flags or (1 shl 12)
        body.writeOctet(properties.deliveryMode)
    }
    if (properties.messageId != null) {
        flags = flags or (1 shl 7)
        body.writeShortstr(properties.messageId)
    }
    return AmqpBuffer().writeShort(flags).toByteArray() + body.toByteArray()
}

internal fun decodeProperties(reader: AmqpReader): ContentProperties {
    if (reader.remaining == 0) return ContentProperties()
    val flags = reader.readShort()
    var contentType: String? = null
    var headers: Map<String, String> = emptyMap()
    var deliveryMode: Int? = null
    var messageId: String? = null

    if ((flags and (1 shl 15)) != 0) contentType = reader.readShortstr()
    if ((flags and (1 shl 14)) != 0) reader.readShortstr() // content-encoding
    if ((flags and (1 shl 13)) != 0) headers = readStringTable(reader)
    if ((flags and (1 shl 12)) != 0) deliveryMode = reader.readOctet()
    if ((flags and (1 shl 11)) != 0) reader.readOctet() // priority
    if ((flags and (1 shl 10)) != 0) reader.readShortstr() // correlation-id
    if ((flags and (1 shl 9)) != 0) reader.readShortstr() // reply-to
    if ((flags and (1 shl 8)) != 0) reader.readShortstr() // expiration
    if ((flags and (1 shl 7)) != 0) messageId = reader.readShortstr()
    if ((flags and (1 shl 6)) != 0) reader.readLongLong() // timestamp
    if ((flags and (1 shl 5)) != 0) reader.readShortstr() // type
    if ((flags and (1 shl 4)) != 0) reader.readShortstr() // user-id
    if ((flags and (1 shl 3)) != 0) reader.readShortstr() // app-id
    if ((flags and (1 shl 2)) != 0) reader.readShortstr() // cluster-id

    return ContentProperties(
        contentType = contentType,
        deliveryMode = deliveryMode,
        headers = headers,
        messageId = messageId,
    )
}

private fun readStringTable(reader: AmqpReader): Map<String, String> {
    val size = reader.readLong()
    val endRemaining = reader.remaining - size
    val result = LinkedHashMap<String, String>()
    while (reader.remaining > endRemaining) {
        val key = reader.readShortstr()
        when (val type = reader.readOctet().toChar()) {
            'S' -> result[key] = reader.readLongstr().decodeToString()
            's' -> result[key] = reader.readShortstr()
            else -> {
                // Skip unknown field values while preserving table bounds roughly via AmqpReader helpers.
                // Re-use skip by wrapping remaining as nested reader is awkward; handle common types.
                when (type) {
                    't', 'b', 'B' -> reader.skip(1)
                    'U', 'u' -> reader.skip(2)
                    'I', 'i', 'f' -> reader.skip(4)
                    'L', 'l', 'd', 'T' -> reader.skip(8)
                    'x' -> reader.skip(reader.readLong())
                    'V' -> Unit
                    'F' -> reader.readTable()
                    'A' -> reader.skip(reader.readLong())
                    'D' -> reader.skip(5)
                    else -> error("Unsupported header field type: $type")
                }
            }
        }
    }
    return result
}
