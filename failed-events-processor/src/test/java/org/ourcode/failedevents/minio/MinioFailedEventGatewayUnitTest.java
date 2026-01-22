package org.ourcode.failedevents.minio;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ourcode.failedevents.api.exception.FailedEventStorageException;
import org.ourcode.failedevents.api.model.FailedEvent;
import org.ourcode.failedevents.minio.configuration.MinioProperties;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MinioFailedEventGatewayUnitTest {

    @Mock
    private MinioClient minioClient;

    private MinioFailedEventGateway target;

    @BeforeEach
    void setUp() {
        MinioProperties minioProperties = new MinioProperties(
                "http://localhost:9000",
                "minioadmin",
                "minioadmin",
                "failed-events",
                true
        );
        ObjectNameGenerator objectNameGenerator = new ObjectNameGenerator();

        target = new MinioFailedEventGateway(
                minioClient,
                minioProperties,
                objectNameGenerator,
                new ObjectMapper()
        );
    }

    /**
     * Given:
     * - Valid failed event
     * - MinioClient throws exception during upload
     * <p>
     * When:
     * - Event is saved via MinioFailedEventGateway
     * <p>
     * Then:
     * - FailedEventStorageException is thrown
     * - Original exception is wrapped
     */
    @Test
    @DisplayName("MinIO client throws exception - FailedEventStorageException is thrown")
    void minioClientThrowsException() throws Exception {
        // Given: valid event
        FailedEvent failedEvent = new FailedEvent(
                "event-789",
                "topic",
                "NullPointerException",
                "{\"data\":\"test\"}",
                Instant.parse("2026-01-13T12:00:00Z")
        );

        // Mock MinioClient to throw exception
        when(minioClient.putObject(any())).thenThrow(new RuntimeException("MinIO connection error"));

        // When/Then: saving the event throws FailedEventStorageException
        assertThatThrownBy(() -> target.save(failedEvent))
                .isInstanceOf(FailedEventStorageException.class)
                .hasMessageContaining("MinIO connection error")
                .hasCauseInstanceOf(RuntimeException.class);
    }

}

