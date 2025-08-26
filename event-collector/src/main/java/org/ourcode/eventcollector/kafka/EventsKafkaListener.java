package org.ourcode.eventcollector.kafka;

import com.ourcode.avro.DeviceEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.ourcode.eventcollector.api.service.DeviceEventCollector;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EventsKafkaListener {

    private final DeviceEventCollector deviceEventCollector;

    public EventsKafkaListener(DeviceEventCollector deviceEventCollector) {
        this.deviceEventCollector = deviceEventCollector;
    }

    @KafkaListener(topics = "events", groupId = "${spring.kafka.consumer.group-id}")
    public void listen(ConsumerRecord<String, DeviceEvent> record, Acknowledgment ack) {
        log.debug("Received event: key = {}, value = {}", record.key(), record.value());

        deviceEventCollector.collect( toModel(record.value()) );
        ack.acknowledge();
    }

    private org.ourcode.eventcollector.api.model.DeviceEvent toModel(DeviceEvent event) {
        return new org.ourcode.eventcollector.api.model.DeviceEvent(
                event.getEventId(), event.getDeviceId(), event.getTimestamp(), event.getType(), event.getPayload()
        );
    }

}
