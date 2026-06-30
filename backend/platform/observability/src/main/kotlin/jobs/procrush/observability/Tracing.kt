package jobs.procrush.observability

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor
import jobs.procrush.bootstrap.config.ObservabilityConfig

object OpenTelemetryFactory {
    private var sdk: OpenTelemetrySdk? = null

    fun create(config: ObservabilityConfig): OpenTelemetry {
        if (!config.otelEnabled) {
            return OpenTelemetry.noop()
        }
        sdk?.let { return it }

        val endpoint = config.otelExporterEndpoint.removePrefix("http://").removePrefix("https://")
        val exporter =
            OtlpGrpcSpanExporter.builder()
                .setEndpoint("http://$endpoint")
                .build()
        val resource =
            Resource.getDefault().merge(
                Resource.builder()
                    .put("service.name", config.serviceName)
                    .put("service.version", config.appVersion)
                    .build(),
            )
        val tracerProvider =
            SdkTracerProvider.builder()
                .addSpanProcessor(BatchSpanProcessor.builder(exporter).build())
                .setResource(resource)
                .build()
        val propagators =
            ContextPropagators.create(
                io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator.getInstance(),
            )
        val openTelemetry =
            OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setPropagators(propagators)
                .build()
        sdk = openTelemetry
        TracePropagation.init(openTelemetry)
        return openTelemetry
    }

    fun shutdown() {
        sdk?.close()
        sdk = null
    }
}

class Tracing(
    private val tracer: Tracer,
) {
    fun startSpan(
        name: String,
        parentContext: io.opentelemetry.context.Context = io.opentelemetry.context.Context.current(),
    ): io.opentelemetry.api.trace.Span {
        val span = tracer.spanBuilder(name).setParent(parentContext).startSpan()
        span.makeCurrent()
        MdcContext.put(CorrelationIds.TRACE_ID, span.spanContext.traceId)
        MdcContext.put(CorrelationIds.SPAN_ID, span.spanContext.spanId)
        return span
    }

    fun endSpan(span: io.opentelemetry.api.trace.Span) {
        span.end()
        MdcContext.clearKeys(CorrelationIds.TRACE_ID, CorrelationIds.SPAN_ID)
    }

    fun <T> span(
        name: String,
        parentContext: io.opentelemetry.context.Context = io.opentelemetry.context.Context.current(),
        block: (io.opentelemetry.api.trace.Span) -> T,
    ): T {
        val span = startSpan(name, parentContext)
        return try {
            block(span)
        } catch (error: Throwable) {
            span.recordException(error)
            span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR)
            throw error
        } finally {
            endSpan(span)
        }
    }

    fun <T> withPropagatedHeaders(
        headers: Map<String, Any?>,
        spanName: String,
        block: () -> T,
    ): T {
        val parentContext = TracePropagation.extractFromMap(headers)
        return span(spanName, parentContext) { block() }
    }

    suspend fun <T> suspendSpan(
        name: String,
        block: suspend () -> T,
    ): T {
        val span = startSpan(name)
        return try {
            block()
        } catch (error: Throwable) {
            span.recordException(error)
            span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR)
            throw error
        } finally {
            endSpan(span)
        }
    }

    fun <T> withKafkaRecord(
        record: org.apache.kafka.clients.consumer.ConsumerRecord<*, *>,
        block: () -> T,
    ): T {
        val parentContext = TracePropagation.extractFromKafka(record)
        return span("kafka.process", parentContext) { block() }
    }

    companion object {
        fun create(
            openTelemetry: OpenTelemetry,
            serviceName: String,
        ): Tracing = Tracing(openTelemetry.getTracer(serviceName))
    }
}
