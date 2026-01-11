package org.ourcode.devicecollector.kafka.consumer;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Deserializer;

import java.util.Map;

/**
 * A resilient Avro deserializer that never throws exceptions.
 * If Avro deserialization fails, it returns null and logs the error.
 * This allows the consumer to continue processing messages even when some are corrupted.
 */
@Slf4j
public abstract class ResilientAvroDeserializer<T> implements Deserializer<T> {

    private final KafkaAvroDeserializer avroDeserializer;

    public ResilientAvroDeserializer() {
        this.avroDeserializer = new KafkaAvroDeserializer();
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        avroDeserializer.configure(configs, isKey);
    }

    @Override
    @SuppressWarnings("unchecked")
    public T deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }

        try {
            return (T) avroDeserializer.deserialize(topic, data);
        } catch (Exception e) {
            log.warn("Failed to deserialize message from topic '{}'. Message will be processed as corrupted. Error: {}",
                    topic, e.getMessage());
            return map(data);
        }
    }

    protected abstract T map(byte[] data);

    @Override
    public void close() {
        avroDeserializer.close();
    }

}

