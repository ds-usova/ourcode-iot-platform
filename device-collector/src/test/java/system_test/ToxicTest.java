package system_test;

import common.AbstractIntegrationTest;
import common.containers.PostgresContainers;
import eu.rekawek.toxiproxy.model.ToxicDirection;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.ourcode.avro.Device;
import org.ourcode.avro.DeviceDeadLetter;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static common.containers.PostgresContainers.PROXY_0;
import static common.containers.PostgresContainers.PROXY_1;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class ToxicTest extends AbstractIntegrationTest {

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        AbstractIntegrationTest.setProperties(registry);
    }

    /**
     * Given:
     * - Both shards of PostgreSQL are not available (simulated using Toxiproxy)
     * <p>
     * When:
     * - Valid device is published to "device-ids" topic
     * <p>
     * Then:
     * - The device is not saved in the database
     * - The device is published to "device-ids-dlt" topic with error message
     */
    @Test
    @DisplayName("Toxic postgres: device is not collected and sent to DLT")
    void deviceIsNotCollected() throws IOException {
        PROXY_0.toxics().limitData("cut_connection", ToxicDirection.DOWNSTREAM, 0);
        PROXY_1.toxics().limitData("cut_connection", ToxicDirection.DOWNSTREAM, 0);

        Device device = Device.newBuilder()
                .setDeviceId("device-1")
                .setDeviceType("sensor")
                .setCreatedAt(System.currentTimeMillis())
                .setMeta("{\"location\":\"warehouse-1\"}")
                .build();

        testProducers.sendDevices(List.of(device));

        Map<String, DeviceDeadLetter> deadLetters = new HashMap<>();
        Awaitility.await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofSeconds(1)).untilAsserted(() -> {
            deadLetters.clear();
            deadLetters.putAll(testConsumers.readDlt());

            assertThat(deadLetters).containsKey("device-1");
        });

        DeviceDeadLetter deadLetter = deadLetters.get(device.getDeviceId());

        assertThat(deadLetter.getDeviceId()).isEqualTo(device.getDeviceId());
        assertThat(deadLetter.getDeviceType()).isEqualTo(device.getDeviceType());
        assertThat(deadLetter.getCreatedAt()).isEqualTo(device.getCreatedAt());
        assertThat(deadLetter.getMeta()).isEqualTo(device.getMeta());

        assertThat(deadLetter.getErrorMessage()).contains("request timed out");
        assertThat(deadLetter.getRawEvent()).isNull();

        PostgresContainers.resetProxies();
    }

}
