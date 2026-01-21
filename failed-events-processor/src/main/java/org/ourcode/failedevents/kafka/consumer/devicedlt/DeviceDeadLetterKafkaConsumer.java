package org.ourcode.failedevents.kafka.consumer.devicedlt;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.ourcode.avro.DeviceDeadLetter;
import org.ourcode.failedevents.api.model.FailedEvent;
import org.ourcode.failedevents.api.service.FailedEventService;
import org.ourcode.failedevents.kafka.configuration.KafkaTopics;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
public class DeviceDeadLetterKafkaConsumer {

    private final KafkaTopics kafkaTopics;
    private final FailedEventService failedEventService;
    private final ExecutorService executor;
    private final ObjectMapper objectMapper;

    public DeviceDeadLetterKafkaConsumer(
            KafkaTopics kafkaTopics,
            FailedEventService failedEventService,
            ObjectMapper objectMapper
    ) {
        this.kafkaTopics = kafkaTopics;
        this.failedEventService = failedEventService;
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.device-ids-dlt}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "deviceDeadLetterKafkaListenerContainerFactory"
    )
    public void listen(
            List<ConsumerRecord<String, DeviceDeadLetter>> records,
            Acknowledgment ack
    ) {
        log.debug("Received {} device dead letter records", records.size());

        List<CompletableFuture<Void>> futures = records.stream()
                .map(record ->
                        CompletableFuture.runAsync(() -> processRecord(record), executor)
                ).toList();

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            ack.acknowledge();
            log.debug("Successfully processed {} device dead letter records", records.size());
        } catch (Exception e) {
            log.error("Failed to process device dead letter records", e);
            throw new RuntimeException("Failed to process one or more device dead letter records", e);
        }
    }

    private void processRecord(ConsumerRecord<String, DeviceDeadLetter> record) {
        FailedEvent failedEvent = toDomain(record.value(), record.timestamp());
        failedEventService.save(failedEvent);
    }

    private FailedEvent toDomain(DeviceDeadLetter deadLetter, long timestamp) {
        return new FailedEvent(
                UUID.randomUUID().toString(),
                kafkaTopics.deviceIdsDlt(),
                deadLetter.getException(),
                generatePayload(deadLetter),
                Instant.ofEpochMilli(timestamp)
        );
    }

    private String generatePayload(DeviceDeadLetter deadLetter) {
        try {
            HashMap<String, Object> payload = new HashMap<>();
            if (deadLetter.getRawEvent() != null) {
                payload.put("rawEvent", deadLetter.getRawEvent());
            } else {
                payload.put("deviceId", deadLetter.getDeviceId());
                payload.put("deviceType", deadLetter.getDeviceType());
                payload.put("createdAt", deadLetter.getCreatedAt().toString()); // Remove L suffix
                payload.put("meta", deadLetter.getMeta());
            }

            payload.put("errorMessage", deadLetter.getErrorMessage());
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.error("Failed to generate payload for dead letter", e);
            return deadLetter.toString();
        }
    }

}

