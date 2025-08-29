package org.ourcode.eventcollector.kafka;

import com.ourcode.avro.DeviceEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.ourcode.eventcollector.api.service.DeviceEventCollector;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EventsKafkaConsumer {

    private final DeviceEventCollector deviceEventCollector;

    public EventsKafkaConsumer(
            DeviceEventCollector deviceEventCollector
    ) {
        this.deviceEventCollector = deviceEventCollector;
    }

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            autoCreateTopics = "true",
            retryTopicSuffix = "-retry",
            dltTopicSuffix = "-dlt",
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            dltStrategy = DltStrategy.ALWAYS_RETRY_ON_ERROR
    )
    @KafkaListener(topics = "${app.kafka.topics.events}", groupId = "${spring.kafka.consumer.group-id}")
    public void listen(
            @Header(KafkaHeaders.RECEIVED_TOPIC) String receivedTopic,
            ConsumerRecord<String, DeviceEvent> record,
            Acknowledgment ack
    ) {
        log.debug("Received event: key = {}, value = {}, topic = {}", record.key(), record.value(), receivedTopic);

        try {
            deviceEventCollector.collect(toModel(record.value()));
            ack.acknowledge();
        } catch (RuntimeException e) {
            log.error("Error processing event", e);
            throw e;
        }
    }

    private org.ourcode.eventcollector.api.model.DeviceEvent toModel(DeviceEvent event) {
        return new org.ourcode.eventcollector.api.model.DeviceEvent(
                event.getEventId(),
                event.getDeviceId(),
                event.getTimestamp(),
                event.getType(),
                event.getPayload()
        );
    }

}
