package system_test;

import common.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.ourcode.avro.DeviceEventDeadLetter;
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

public class EventDltCollectionTest extends AbstractIntegrationTest {

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        AbstractIntegrationTest.setProperties(registry);
    }

    @Autowired
    private KafkaTopics kafkaTopics;

    /**
     * Given:
     * - Valid DeviceEventDeadLetter is sent to "events-dlt" topic
     * - Exception is "NullPointerException"
     * When:
     * - The Failed Events Processor processes the dead letter
     * Then:
     * - The dead letter is logged in MinIO under the folder with "events-dlt/NullPointerException/" prefix
     */
    @Test
    @DisplayName("happy path - when an event dl without raw field is sent - then it's saved in MinIO")
    void processorHandlesValidEventDeadLetter() {
        String exception = "NullPointerException";
        DeviceEventDeadLetter dl = DeviceEventDeadLetter.newBuilder()
                .setEventId("event-123")
                .setDeviceId("device-456")
                .setTimestamp(1692000000000L)
                .setType("temperature")
                .setPayload("{\"value\":25.5,\"unit\":\"celsius\"}")
                .setException(exception)
                .setErrorMessage("Simulated error for testing")
                .build();

        testProducers.sendEventDeadLetters(List.of(dl));

        List<String> npeFiles = new ArrayList<>();
        Awaitility.await().atMost(Duration.ofSeconds(10)).pollInterval(Duration.ofSeconds(2)).untilAsserted(() -> {
            npeFiles.clear();
            npeFiles.addAll(
                    testMinioClient.listObjects(kafkaTopics.eventsDlt() + "/" + exception)
            );

            assertThat(npeFiles).hasSize(1);
        });

        Map<String, Object> payload = testMinioClient.readObjectAsJson(npeFiles.getFirst());

        assertThat(payload)
                .containsEntry("eventId", "event-123")
                .containsEntry("deviceId", "device-456")
                .containsEntry("timestamp", "1692000000000")
                .containsEntry("type", "temperature")
                .containsEntry("payload", "{\"value\":25.5,\"unit\":\"celsius\"}")
                .containsEntry("errorMessage", "Simulated error for testing");
    }

    /**
     * Given:
     * - Event that couldn't be deserialized is sent to "events-dlt" topic
     * - Exception is "DeserializationException"
     * When:
     * - The Failed Events Processor processes the dead letter
     * Then:
     * - The dead letter is logged in MinIO under the folder with "events-dlt/DeserializationException/" prefix
     */
    @Test
    @DisplayName("happy path - when an event dl with raw field is sent - then it's saved in MinIO")
    void processorHandlesDeserializationException() {
        String exception = "DeserializationException";
        DeviceEventDeadLetter dl = DeviceEventDeadLetter.newBuilder()
                .setRawEvent(Base64.getEncoder().encodeToString("poison-pill".getBytes()))
                .setException(exception)
                .setErrorMessage("Record could not be deserialized")
                .build();

        testProducers.sendEventDeadLetters(List.of(dl));

        List<String> npeFiles = new ArrayList<>();
        Awaitility.await().atMost(Duration.ofSeconds(10)).pollInterval(Duration.ofSeconds(2)).untilAsserted(() -> {
            npeFiles.clear();
            npeFiles.addAll(
                    testMinioClient.listObjects(kafkaTopics.eventsDlt() + "/" + exception)
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
     * - Poison pill was published to "events-dlt" topic
     * When:
     * - The Failed Events Processor processes the dead letter
     * Then:
     * - The poison pill is mapped to a valid DeviceEventDeadLetter and saved in MinIO under the folder with "events-dlt/DeserializationException/" prefix
     */
    @Test
    @DisplayName("unhappy path - when a poison pill is sent to event DLT - then it's still saved in MinIO")
    void eventDltIsProcessed() {
        testProducers.sendRawEventBytes(kafkaTopics.eventsDlt(), "poison-pill-key", "poison-pill".getBytes());

        List<String> npeFiles = new ArrayList<>();
        Awaitility.await().atMost(Duration.ofSeconds(10)).pollInterval(Duration.ofSeconds(2)).untilAsserted(() -> {
            npeFiles.clear();
            npeFiles.addAll(
                    testMinioClient.listObjects(kafkaTopics.eventsDlt() + "/DeserializationException")
            );

            assertThat(npeFiles).hasSize(1);
        });

        Map<String, Object> payload = testMinioClient.readObjectAsJson(npeFiles.getFirst());

        assertThat(payload)
                .containsEntry("rawEvent", "cG9pc29uLXBpbGw=")
                .containsEntry("errorMessage", "Corrupted message - unable to deserialize dlt");
    }

}
