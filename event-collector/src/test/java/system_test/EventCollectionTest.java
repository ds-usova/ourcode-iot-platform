package system_test;

import common.AbstractIntegrationTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.ourcode.avro.Device;
import org.ourcode.avro.DeviceEvent;
import org.ourcode.avro.DeviceEventDeadLetter;
import org.ourcode.eventcollector.cassandra.DeviceEventEntity;
import org.ourcode.eventcollector.cassandra.DeviceEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class EventCollectionTest extends AbstractIntegrationTest {

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        AbstractIntegrationTest.setProperties(registry);
    }

    @Autowired
    protected DeviceEventRepository deviceEventRepository;

    /**
     * Given:
     * - DeviceEvent Avro schema is registered in Schema Registry
     * - Device Avro schema is registered in Schema Registry
     * <p>
     * When:
     * - Two events with the same device id are published to Kafka "events" topic
     * <p>
     * Then:
     * - Both events are saved in Cassandra
     * - Only one message is published to "device-ids" topic
     */
    @Test
    @DisplayName("Happy path: event is collected and device is registered only once")
    void eventIsCollected() {
        DeviceEvent event1 = DeviceEvent.newBuilder()
                .setEventId("event-1")
                .setDeviceId("device-1")
                .setTimestamp(System.currentTimeMillis())
                .setType("TEMPERATURE")
                .setPayload("{\"temp\":25}")
                .build();

        DeviceEvent event2 = DeviceEvent.newBuilder()
                .setEventId("event-2")
                .setDeviceId("device-1")
                .setTimestamp(System.currentTimeMillis())
                .setType("TEMPERATURE")
                .setPayload("{\"temp\":20}")
                .build();

        testProducers.sendEvents(List.of(event1, event2));

        Awaitility.await().atMost(Duration.ofSeconds(10)).pollInterval(Duration.ofSeconds(1)).untilAsserted(() -> {
            List<DeviceEventEntity> savedEvents = deviceEventRepository.findAll();

            assertThat(savedEvents).hasSize(2);
            assertThat(savedEvents)
                    .extracting(DeviceEventEntity::getEventId)
                    .containsExactlyInAnyOrder("event-1", "event-2");

            Map<String, Device> devices = testConsumers.readDevices();
            assertThat(devices).withFailMessage("Expected exactly one device to be registered").hasSize(1);
            assertThat(devices.containsKey("device-1")).isTrue();
            assertThat(devices.get("device-1").getDeviceId()).isEqualTo("device-1");
        });
    }

    /**
     * Given:
     * - Invalid Avro message is sent to the Kafka "events" topic
     * <p>
     * When:
     * - The message is consumed
     * <p>
     * Then:
     * - The message is sent to the DLT topic with an error message
     * - No event is saved in Cassandra
     */
    @Test
    @DisplayName("Invalid Avro: event is not collected if schema is invalid")
    void eventIsNotCollectedOnInvalidAvro() {
        // Given: payload that does not conform to the Avro schema
        byte[] invalidPayload = "poison-pills".getBytes();

        // When: Send invalid Avro bytes to the events topic
        testProducers.sendRawEventBytes("invalid-key", invalidPayload);

        // Then: The event is sent to the DLT
        Awaitility.await().atMost(Duration.ofSeconds(5)).pollInterval(Duration.ofSeconds(1)).untilAsserted(() -> {
            Map<String, DeviceEventDeadLetter> devices = testConsumers.readDlt();
            assertThat(devices).withFailMessage("Expected exactly one event in DLT").hasSize(1);
            assertThat(devices.containsKey("invalid-key")).isTrue();
            assertThat(devices.get("invalid-key").getErrorMessage()).isEqualTo("failed to deserialize");
            assertThat(devices.get("invalid-key").getRawEvent()).isEqualTo(Base64.getEncoder().encodeToString(invalidPayload));

            List<DeviceEventEntity> savedEvents = deviceEventRepository.findAll();
            assertThat(savedEvents).isEmpty();
        });
    }

}
