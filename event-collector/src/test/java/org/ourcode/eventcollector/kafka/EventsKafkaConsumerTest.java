package org.ourcode.eventcollector.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.ourcode.avro.DeviceEvent;
import org.ourcode.eventcollector.api.service.DeviceEventCollector;
import org.springframework.kafka.support.Acknowledgment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EventsKafkaConsumerTest {
    private EventsKafkaConsumer eventsKafkaConsumer;
    private DeviceEventCollector deviceEventCollector;

    @BeforeEach
    void setUp() {
        deviceEventCollector = mock(DeviceEventCollector.class);
        eventsKafkaConsumer = new EventsKafkaConsumer(deviceEventCollector);
    }

    @Test
    @DisplayName("listen should map DeviceEvent correctly and call collect")
    void listen_MapsDeviceEventCorrectly() {
        // Given: an Avro DeviceEvent is in correct format
        String eventId = "event-123";
        String deviceId = "device-456";
        long timestamp = 123456789L;
        String type = "TEMPERATURE";
        String payload = "{\"temp\":25}";

        DeviceEvent avroEvent = DeviceEvent.newBuilder()
                .setDeviceId(deviceId)
                .setEventId(eventId)
                .setType(type)
                .setTimestamp(timestamp)
                .setPayload(payload)
                .build();

        ConsumerRecord<String, DeviceEvent> record = new ConsumerRecord<>("events", 0, 0L, "key", avroEvent);
        Acknowledgment ack = mock(Acknowledgment.class);

        // When: calling listen
        eventsKafkaConsumer.listen("topic", record, ack);

        // Then: collect called with correct mapping, and ack acknowledged
        ArgumentCaptor<org.ourcode.eventcollector.api.model.DeviceEvent> captor = ArgumentCaptor.forClass(org.ourcode.eventcollector.api.model.DeviceEvent.class);
        verify(deviceEventCollector).collect(captor.capture());

        org.ourcode.eventcollector.api.model.DeviceEvent mapped = captor.getValue();
        assertThat(mapped.eventId()).isEqualTo(eventId);
        assertThat(mapped.deviceId()).isEqualTo(deviceId);
        assertThat(mapped.timestamp()).isEqualTo(timestamp);
        assertThat(mapped.eventType()).isEqualTo(type);
        assertThat(mapped.payload()).isEqualTo(payload);

        verify(ack).acknowledge();
    }
}