package integration.minio;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ourcode.failedevents.api.exception.FailedEventStorageException;
import org.ourcode.failedevents.api.gateway.FailedEventGateway;
import org.ourcode.failedevents.api.model.FailedEvent;
import org.ourcode.failedevents.minio.MinioFailedEventGateway;
import org.ourcode.failedevents.minio.ObjectNameGenerator;
import org.ourcode.failedevents.minio.configuration.MinioProperties;
import org.ourcode.failedevents.retry.LoggingRetryListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.RetryListener;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Integration test for MinioFailedEventGateway retry logic.
 * <p>
 * MinioClient is mocked to simulate failures.
 * Context includes only necessary beans for the gateway:
 * - MinioFailedEventGateway (target bean)
 * - MinioClient (mocked)
 * - MinioProperties, ObjectNameGenerator, ObjectMapper (dependencies)
 * - @EnableRetry support (for retry mechanism)
 */
@Slf4j
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = MinioFailedEventGatewayRetryTest.TestConfig.class)
@TestPropertySource(properties = {
        "spring.retry.minio-gateway.max-attempts=3",
        "spring.retry.minio-gateway.backoff-delay=100"
})
class MinioFailedEventGatewayRetryTest {

    @MockitoBean
    private MinioClient minioClient;

    @Autowired
    private FailedEventGateway target;

    /**
     * Given:
     * - Retry is configured for 3 max attempts
     * - MinioClient fails on first two attempts but succeeds on third
     * <p>
     * When:
     * - save() is called
     * <p>
     * Then:
     * - Method should succeed after retries
     * - putObject should be called 3 times
     */
    @Test
    @DisplayName("happy path - when MinIO fails twice then succeeds - then retry succeeds on third attempt")
    void shouldSucceedOnThirdAttemptAfterRetries() throws Exception {
        // Given: MinIO fails twice then succeeds
        when(minioClient.putObject(any(PutObjectArgs.class)))
                .thenThrow(new RuntimeException("Connection timeout"))
                .thenThrow(new RuntimeException("Network error"))
                .thenReturn(null);

        FailedEvent failedEvent = new FailedEvent(
                "event-123",
                "device-ids-dlt",
                "NullPointerException",
                "{\"temperature\":25.5}",
                Instant.parse("2026-01-13T10:30:45Z")
        );

        // When: saving the event
        assertThatCode(() -> target.save(failedEvent))
                .doesNotThrowAnyException();

        // Then: putObject was called 3 times (2 failures + 1 success)
        verify(minioClient, times(3)).putObject(any(PutObjectArgs.class));
    }

    /**
     * Given:
     * - Retry is configured for 3 max attempts
     * - MinioClient fails on all attempts
     * <p>
     * When:
     * - save() is called
     * <p>
     * Then:
     * - FailedEventStorageException should be thrown after all retries exhausted
     * - putObject should be called 3 times
     */
    @Test
    @DisplayName("unhappy path - when MinIO fails on all retry attempts - then throw FailedEventStorageException")
    void shouldThrowExceptionWhenAllRetriesExhausted() throws Exception {
        // Given: MinIO always fails
        when(minioClient.putObject(any(PutObjectArgs.class)))
                .thenThrow(new RuntimeException("MinIO unavailable"));

        FailedEvent failedEvent = new FailedEvent(
                "event-123",
                "device-ids-dlt",
                "NullPointerException",
                "{\"temperature\":25.5}",
                Instant.parse("2026-01-13T10:30:45Z")
        );

        // When/Then: exception is thrown after all retries
        assertThatThrownBy(() -> target.save(failedEvent))
                .isInstanceOf(FailedEventStorageException.class)
                .hasMessageContaining("MinIO unavailable")
                .hasCauseInstanceOf(RuntimeException.class);

        // Then: putObject was called exactly 3 times (max attempts)
        verify(minioClient, times(3)).putObject(any(PutObjectArgs.class));
    }

    /**
     * Given:
     * - MinioClient succeeds on first attempt
     * <p>
     * When:
     * - save() is called
     * <p>
     * Then:
     * - No retry should occur
     * - putObject should be called only once
     */
    @Test
    @DisplayName("happy path - when MinIO succeeds on first attempt - then no retry occurs")
    void shouldNotRetryWhenFirstAttemptSucceeds() throws Exception {
        // Given: MinIO succeeds immediately
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);

        FailedEvent failedEvent = new FailedEvent(
                "event-123",
                "device-ids-dlt",
                "NullPointerException",
                "{\"temperature\":25.5}",
                Instant.parse("2026-01-13T10:30:45Z")
        );

        // When: saving the event
        assertThatCode(() -> target.save(failedEvent))
                .doesNotThrowAnyException();

        // Then: putObject was called only once (no retry needed)
        verify(minioClient, times(1)).putObject(any(PutObjectArgs.class));
    }

    @EnableRetry
    @Configuration
    static class TestConfig {

        @Bean
        public FailedEventGateway minioFailedEventGateway(
                MinioClient minioClient, // will be mocked
                MinioProperties minioProperties,
                ObjectNameGenerator objectNameGenerator,
                ObjectMapper objectMapper
        ) {
            return new MinioFailedEventGateway(minioClient, minioProperties, objectNameGenerator, objectMapper);
        }

        @Bean
        public MinioProperties minioProperties() {
            return new MinioProperties(
                    "http://localhost:9000",
                    "minioadmin",
                    "minioadmin",
                    "failed-events",
                    false // autoCreateBucket not needed for retry tests
            );
        }

        @Bean
        public ObjectNameGenerator objectNameGenerator() {
            return new ObjectNameGenerator();
        }

        @Bean
        public ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        public RetryListener loggingRetryListener() {
            return new LoggingRetryListener();
        }
    }

}
