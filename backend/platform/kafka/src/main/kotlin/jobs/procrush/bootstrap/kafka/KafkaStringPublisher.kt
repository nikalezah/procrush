package jobs.procrush.bootstrap.kafka

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata

class KafkaStringPublisher(
    private val producer: KafkaProducer<String, String>,
    private val topic: String,
) {
    fun publish(
        key: String,
        body: String,
        configure: (ProducerRecord<String, String>) -> Unit = {},
        onCompletion: (RecordMetadata?, Exception?) -> Unit = { _, _ -> },
    ) {
        val record = ProducerRecord(topic, key, body)
        configure(record)
        producer.send(record) { metadata, error -> onCompletion(metadata, error) }
    }

    fun flush() {
        producer.flush()
    }
}
