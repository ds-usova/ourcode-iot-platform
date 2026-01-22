package integration.minio;

import common.AbstractIntegrationTest;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.ourcode.failedevents.api.model.FailedEvent;
import org.ourcode.failedevents.minio.MinioFailedEventGateway;
import org.ourcode.failedevents.minio.configuration.MinioProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.InputStream;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class MinioFailedEventGatewayIntegrationTest extends AbstractIntegrationTest {

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        AbstractIntegrationTest.setProperties(registry);
    }

    @Autowired
    private MinioFailedEventGateway target;

    @Autowired
    private MinioProperties minioProperties;

    @Autowired
    private MinioClient minioClient;

    /**
     * Given:
     * - Valid failed event with JSON payload
     * <p>
     * When:
     * - Event is saved via MinioFailedEventGateway
     * <p>
     * Then:
     * - Object is stored in MinIO bucket
     * - Object has correct content type (application/json)
     * - Object has correct payload content
     * - Object name follows expected pattern
     */
    @Test
    @DisplayName("happy path - when failed event has JSON payload - then save it correctly")
    void saveFailedEventWithJsonPayload() throws Exception {
        // Given: failed event with JSON payload
        String eventId = UUID.randomUUID().toString();
        String jsonPayload = "{\"temperature\":25.5,\"humidity\":60}";
        Instant timestamp = Instant.parse("2026-01-13T10:30:45Z");

        FailedEvent failedEvent = new FailedEvent(
                eventId,
                "device-ids-dlt",
                "NullPointerException",
                jsonPayload,
                timestamp
        );

        // When: saving the event
        target.save(failedEvent);

        // Then: object is stored in MinIO
        String expectedObjectName = "device-ids-dlt/NullPointerException/2026/1/13/10-30-45-" + eventId + ".json";

        InputStream objectStream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(minioProperties.bucket())
                        .object(expectedObjectName)
                        .build()
        );

        String actualPayload = new String(objectStream.readAllBytes());
        assertThat(actualPayload).isEqualTo(jsonPayload);
    }

    /**
     * Given:
     * - Valid failed event with non-JSON payload (plain text)
     * <p>
     * When:
     * - Event is saved via MinioFailedEventGateway
     * <p>
     * Then:
     * - Object is stored in MinIO bucket
     * - Object has correct content type (text/plain)
     * - Object has correct payload content
     * - Object name follows expected pattern
     */
    @Test
    @DisplayName("happy path - when failed event has non-JSON payload - then save it correctly")
    void saveFailedEventWithNonJsonPayload() throws Exception {
        // Given: failed event with plain text payload
        String eventId = UUID.randomUUID().toString();
        String textPayload = "This is a plain text payload";
        Instant timestamp = Instant.parse("2026-01-13T15:45:30Z");

        FailedEvent failedEvent = new FailedEvent(
                eventId,
                "device-ids-dlt",
                "NullPointerException",
                textPayload,
                timestamp
        );

        // When: saving the event
        target.save(failedEvent);

        // Then: object is stored in MinIO
        String expectedObjectName = "device-ids-dlt/NullPointerException/2026/1/13/15-45-30-" + eventId + ".txt";

        InputStream objectStream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(minioProperties.bucket())
                        .object(expectedObjectName)
                        .build()
        );

        String actualPayload = new String(objectStream.readAllBytes());
        assertThat(actualPayload).isEqualTo(textPayload);
    }

}
