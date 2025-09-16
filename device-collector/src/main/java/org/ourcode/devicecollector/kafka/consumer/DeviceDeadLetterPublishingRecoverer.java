package org.ourcode.devicecollector.kafka.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.ourcode.avro.Device;
import org.ourcode.avro.DeviceDeadLetter;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.lang.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
public class DeviceDeadLetterPublishingRecoverer extends DeadLetterPublishingRecoverer {

    public DeviceDeadLetterPublishingRecoverer(KafkaTemplate<String, Object> kafkaTemplate) {
        super(kafkaTemplate);
    }

    @Override
    protected ProducerRecord<Object, Object> createProducerRecord(
            ConsumerRecord<?, ?> record,
            TopicPartition topicPartition,
            Headers headers,
            @Nullable byte[] key,
            @Nullable byte[] value
    ) {
        Header exceptionMessageHeader = headers.lastHeader(KafkaHeaders.DLT_EXCEPTION_MESSAGE);
        String errorMessage = (exceptionMessageHeader != null && exceptionMessageHeader.value() != null)
                ? new String(exceptionMessageHeader.value(), StandardCharsets.UTF_8) : "N/A";

        DeviceDeadLetter.Builder builder = DeviceDeadLetter.newBuilder().setErrorMessage(errorMessage);

        DeviceDeadLetter deadLetter;
        if (record.value() instanceof Device device) {
            deadLetter = builder.setDeviceId(device.getDeviceId())
                    .setDeviceType(device.getDeviceType())
                    .setCreatedAt(device.getCreatedAt())
                    .setMeta(device.getMeta())
                    .setRawEvent(null)
                    .build();
        } else {
            deadLetter = builder.setRawEvent(value != null ? Base64.getEncoder().encodeToString(value) : null).build();
        }

        log.error("Publishing to device DLT topic {}: {}", topicPartition.topic(), deadLetter);
        return new ProducerRecord<>(
                topicPartition.topic(),
                topicPartition.partition() < 0 ? null : topicPartition.partition(),
                record.key() != null ? record.key().toString() : null,
                deadLetter,
                headers
        );
    }

}
