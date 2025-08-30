package system_test;

import com.ourcode.avro.Device;
import com.ourcode.avro.DeviceEvent;
import common.AbstractIntegrationTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.ourcode.eventcollector.cassandra.DeviceEventEntity;
import org.ourcode.eventcollector.cassandra.DeviceEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;


@Slf4j
public class EventCollectionTest extends AbstractIntegrationTest {

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

}


