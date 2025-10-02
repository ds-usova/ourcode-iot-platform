package system_test;

import common.AbstractIntegrationTest;
import eu.rekawek.toxiproxy.model.ToxicDirection;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.ourcode.avro.DeviceEvent;
import org.ourcode.avro.DeviceEventDeadLetter;
import org.ourcode.eventcollector.cassandra.DeviceEventEntity;
import org.ourcode.eventcollector.cassandra.DeviceEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class ToxicTest extends AbstractIntegrationTest {

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        AbstractIntegrationTest.setProperties(registry, true);
    }

    @Autowired
    protected DeviceEventRepository deviceEventRepository;

    /**
     * Given:
     * - Cassandra call fails due to network issues
     * <p>
     * When:
     * - Valid device event is published to Kafka "events" topic
     * <p>
     * Then:
     * - The event is not saved in Cassandra
     * - The event is published to "events-dlt" topic with error message
     */
    @Test
    @DisplayName("Toxic cassandra: event is not collected and sent to DLT")
    void eventIsCollected() throws IOException {
        cassandraProxy.toxics().latency("latency", ToxicDirection.DOWNSTREAM, 3000);

        DeviceEvent event1 = DeviceEvent.newBuilder()
                .setEventId("event-1")
                .setDeviceId("device-1")
                .setTimestamp(System.currentTimeMillis())
                .setType("TEMPERATURE")
                .setPayload("{\"temp\":25}")
                .build();

        testProducers.sendEvents(List.of(event1));

        Awaitility.await().atMost(Duration.ofSeconds(10)).pollInterval(Duration.ofSeconds(1)).untilAsserted(() -> {
            Map<String, DeviceEventDeadLetter> deadLetters = testConsumers.readDlt();
            assertThat(deadLetters).withFailMessage("Expected one message in DLT").hasSize(1);
            assertThat(deadLetters.containsKey("event-1")).isTrue();

            DeviceEventDeadLetter deadLetter = deadLetters.get("event-1");
            assertThat(deadLetter.getEventId()).isEqualTo("event-1");
            assertThat(deadLetter.getDeviceId()).isEqualTo("device-1");
            assertThat(deadLetter.getType()).isEqualTo("TEMPERATURE");
            assertThat(deadLetter.getPayload()).isEqualTo("{\"temp\":25}");
            assertThat(deadLetter.getErrorMessage()).contains("Query timed out");
            assertThat(deadLetter.getRawEvent()).isNull();
            assertThat(deadLetter.getTimestamp()).isNotNull();
        });

        cassandraProxy.toxics().get("latency").remove();

        List<DeviceEventEntity> savedEvents = deviceEventRepository.findAll();
        assertThat(savedEvents).hasSize(0);
    }

}
