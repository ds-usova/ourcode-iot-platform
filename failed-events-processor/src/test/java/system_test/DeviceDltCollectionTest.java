package system_test;

import common.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.ourcode.avro.DeviceDeadLetter;
import org.ourcode.failedevents.kafka.configuration.KafkaTopics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class DeviceDltCollectionTest extends AbstractIntegrationTest {

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        AbstractIntegrationTest.setProperties(registry);
    }

    @Autowired
    private KafkaTopics kafkaTopics;

    /**
     * Given:
     * - Valid DeviceDeadLetter is sent to "device-ids-dlt" topic
     * - Exception is "NullPointerException"
     * When:
     * - The Failed Events Processor processes the dead letter
     * Then:
     * - The dead letter is logged in MinIO under the folder with "device-ids-dlt/NullPointerException/" prefix
     */
    @Test
    @DisplayName("happy path - when a device dl without raw field is sent - then it's saved in MinIO")
    void processorHandlesValidDeviceDeadLetter() {
        String exception = "NullPointerException";
        DeviceDeadLetter dl = DeviceDeadLetter.newBuilder()
                .setDeviceId("device-123")
                .setDeviceType("sensor")
                .setCreatedAt(1692000000000L)
                .setMeta("{\"location\":\"warehouse-1\"}")
                .setException(exception)
                .setErrorMessage("Simulated error for testing")
                .build();

        testProducers.sendDeviceDeadLetters(List.of(dl));

        List<String> npeFiles = new ArrayList<>();
        Awaitility.await().atMost(Duration.ofSeconds(10)).pollInterval(Duration.ofSeconds(2)).untilAsserted(() -> {
            npeFiles.clear();
            npeFiles.addAll(
                    testMinioClient.listObjects(kafkaTopics.deviceIdsDlt() + "/" + exception)
            );

            assertThat(npeFiles).hasSize(1);
        });

        Map<String, Object> payload = testMinioClient.readObjectAsJson(npeFiles.getFirst());

        assertThat(payload)
                .containsEntry("deviceId", "device-123")
                .containsEntry("deviceType", "sensor")
                .containsEntry("createdAt", "1692000000000")
                .containsEntry("meta", "{\"location\":\"warehouse-1\"}")
                .containsEntry("errorMessage", "Simulated error for testing");
    }

    /**
     * Given:
     * - Device that couldn't be deserialized is sent to "device-ids-dlt" topic
     * - Exception is "DeserializationException"
     * When:
     * - The Failed Events Processor processes the dead letter
     * Then:
     * - The dead letter is logged in MinIO under the folder with "device-ids-dlt/DeserializationException/" prefix
     */
    @Test
    @DisplayName("happy path - when a device dl with raw field is sent - then it's saved in MinIO")
    void processorHandlesDeserializationException() {
        String exception = "DeserializationException";
        DeviceDeadLetter dl = DeviceDeadLetter.newBuilder()
                .setRawEvent(Base64.getEncoder().encodeToString("poison-pill".getBytes()))
                .setException(exception)
                .setErrorMessage("Record could not be deserialized")
                .build();

        testProducers.sendDeviceDeadLetters(List.of(dl));

        List<String> npeFiles = new ArrayList<>();
        Awaitility.await().atMost(Duration.ofSeconds(10)).pollInterval(Duration.ofSeconds(2)).untilAsserted(() -> {
            npeFiles.clear();
            npeFiles.addAll(
                    testMinioClient.listObjects(kafkaTopics.deviceIdsDlt() + "/" + exception)
            );

            assertThat(npeFiles).hasSize(1);
        });

        Map<String, Object> payload = testMinioClient.readObjectAsJson(npeFiles.getFirst());

        assertThat(payload)
                .containsEntry("rawEvent", "cG9pc29uLXBpbGw=")
                .containsEntry("errorMessage", "Record could not be deserialized");
    }

    /**
     * Given:
     * - Poison pill was published to "device-ids-dlt" topic
     * When:
     * - The Failed Events Processor processes the dead letter
     * Then:
     * - The poison pill is mapped to a valid DeviceDeadLetter and saved in MinIO under the folder with "device-ids-dlt/DeserializationException/" prefix
     */
    @Test
    @DisplayName("unhappy path - when a poison pill is sent to device DLT - then it's still saved in MinIO")
    void deviceDltIsProcessed() {
        testProducers.sendRawEventBytes(kafkaTopics.deviceIdsDlt(), "poison-pill-key", "poison-pill".getBytes());

        List<String> npeFiles = new ArrayList<>();
        Awaitility.await().atMost(Duration.ofSeconds(10)).pollInterval(Duration.ofSeconds(2)).untilAsserted(() -> {
            npeFiles.clear();
            npeFiles.addAll(
                    testMinioClient.listObjects(kafkaTopics.deviceIdsDlt() + "/DeserializationException")
            );

            assertThat(npeFiles).hasSize(1);
        });

        Map<String, Object> payload = testMinioClient.readObjectAsJson(npeFiles.getFirst());

        assertThat(payload)
                .containsEntry("rawEvent", "cG9pc29uLXBpbGw=")
                .containsEntry("errorMessage", "Corrupted message - unable to deserialize dlt");
    }

}
