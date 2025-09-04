package org.ourcode.eventcollector.kafka.configuration.consumer;

import com.ourcode.avro.DeviceEvent;
import com.ourcode.avro.DeviceEventDeadLetter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.lang.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
public class DeviceEventDeadLetterPublishingRecoverer extends DeadLetterPublishingRecoverer {

    public DeviceEventDeadLetterPublishingRecoverer(KafkaTemplate<String, Object> kafkaTemplate) {
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

        DeviceEventDeadLetter.Builder builder = DeviceEventDeadLetter.newBuilder().setErrorMessage(errorMessage);

        DeviceEventDeadLetter deadLetter;
        if (record.value() instanceof DeviceEvent event) {
            deadLetter = builder.setEventId(event.getEventId())
                    .setDeviceId(event.getDeviceId())
                    .setTimestamp(event.getTimestamp())
                    .setType(event.getType())
                    .setPayload(event.getPayload())
                    .setRawEvent(null)
                    .build();
        } else {
            deadLetter = builder.setRawEvent(value != null ? Base64.getEncoder().encodeToString(value) : null).build();
        }

        log.error("Publishing to DLT topic {}: {}", topicPartition.topic(), deadLetter);
        return new ProducerRecord<>(
                topicPartition.topic(),
                topicPartition.partition() < 0 ? null : topicPartition.partition(),
                record.key() != null ? record.key().toString() : null,
                deadLetter,
                headers
        );
    }

}
