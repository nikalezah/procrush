package jobs.procrush.observability

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.context.Context
import io.opentelemetry.context.propagation.TextMapGetter
import io.opentelemetry.context.propagation.TextMapPropagator
import io.opentelemetry.context.propagation.TextMapSetter
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.Headers
import java.nio.charset.StandardCharsets

object TracePropagation {
    private lateinit var propagator: TextMapPropagator

    fun init(openTelemetry: OpenTelemetry) {
        propagator = openTelemetry.propagators.textMapPropagator
    }

    fun injectCurrent(record: ProducerRecord<*, *>) {
        injectCurrent(record.headers())
    }

    fun injectCurrent(headers: Headers) {
        if (!::propagator.isInitialized) return
        propagator.inject(Context.current(), headers, KafkaHeadersSetter)
    }

    fun extractFromKafka(record: ConsumerRecord<*, *>): Context {
        if (!::propagator.isInitialized) return Context.current()
        return propagator.extract(Context.current(), record.headers(), KafkaHeadersGetter)
    }

    fun extractFromMap(headers: Map<String, Any?>): Context {
        if (!::propagator.isInitialized) return Context.current()
        return propagator.extract(
            Context.current(),
            headers,
            MapTextMapGetter,
        )
    }

    fun injectIntoMap(headers: MutableMap<String, String>) {
        if (!::propagator.isInitialized) return
        propagator.inject(Context.current(), headers, MapSetter)
    }

    fun requestIdFromMap(headers: Map<String, Any?>): String? =
        headers[CorrelationIds.HEADER_REQUEST_ID]?.toString()
            ?: headers["x-request-id"]?.toString()

    private object KafkaHeadersSetter : TextMapSetter<Headers> {
        override fun set(
            carrier: Headers?,
            key: String,
            value: String,
        ) {
            carrier?.add(key, value.toByteArray(StandardCharsets.UTF_8))
        }
    }

    private object MapTextMapGetter : TextMapGetter<Map<String, Any?>> {
        override fun keys(carrier: Map<String, Any?>): Iterable<String> = carrier.keys

        override fun get(
            carrier: Map<String, Any?>?,
            key: String,
        ): String? = carrier?.get(key)?.toString()
    }

    private object KafkaHeadersGetter : TextMapGetter<Headers> {
        override fun keys(carrier: Headers): Iterable<String> = carrier.map { it.key() }

        override fun get(
            carrier: Headers?,
            key: String,
        ): String? = carrier?.lastHeader(key)?.value()?.toString(StandardCharsets.UTF_8)
    }

    private object MapSetter : TextMapSetter<MutableMap<String, String>> {
        override fun set(
            carrier: MutableMap<String, String>?,
            key: String,
            value: String,
        ) {
            carrier?.put(key, value)
        }
    }
}
