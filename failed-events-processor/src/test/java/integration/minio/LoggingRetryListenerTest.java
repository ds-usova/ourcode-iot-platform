package integration.minio;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import org.slf4j.LoggerFactory;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Integration test LoggingRetryListener: verifies that retry attempts
 * are logged correctly during MinioFailedEventGateway operations.
 */
@Slf4j
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = LoggingRetryListenerTest.TestConfig.class)
@TestPropertySource(properties = {
        "spring.retry.minio-gateway.max-attempts=3",
        "spring.retry.minio-gateway.backoff-delay=100"
})
class LoggingRetryListenerTest {

    @MockitoBean
    private MinioClient minioClient;

    @Autowired
    private FailedEventGateway target;

    private ListAppender<ILoggingEvent> logAppender;
    private Logger retryListenerLogger;

    @BeforeEach
    void setUp() {
        retryListenerLogger = (Logger) LoggerFactory.getLogger(LoggingRetryListener.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        retryListenerLogger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        retryListenerLogger.detachAppender(logAppender);
        logAppender.stop();
    }

    /**
     * Given:
     * - Retry is configured for 3 max attempts
     * - MinioClient fails on first two attempts but succeeds on third
     * <p>
     * When:
     * - save() is called
     * <p>
     * Then:
     * - Should log 2 WARN messages for failed attempts
     * - Should log 1 INFO message for successful completion after 3 attempts
     */
    @Test
    @DisplayName("happy path - when operation succeeds after retries - then log WARN for each failure and INFO on success")
    void shouldLogRetryAttemptsAndSuccess() throws Exception {
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
        target.save(failedEvent);

        // Then: three log events were reported: 2 WARNs (failures) + 1 INFO (success)
        List<ILoggingEvent> logEvents = logAppender.list;
        assertThat(logEvents).hasSize(3);

        // First retry attempt failure (attempt 1): Connection timeout
        ILoggingEvent firstRetry = logEvents.getFirst();
        assertThat(firstRetry.getLevel()).isEqualTo(Level.WARN);
        assertThat(firstRetry.getFormattedMessage()).isEqualTo("Retry attempt 1/3 failed due to: Connection timeout");

        // Second retry attempt failure (attempt 2): Network error
        ILoggingEvent secondRetry = logEvents.get(1);
        assertThat(secondRetry.getLevel()).isEqualTo(Level.WARN);
        assertThat(secondRetry.getFormattedMessage()).isEqualTo("Retry attempt 2/3 failed due to: Network error");

        // Success after retries
        ILoggingEvent successLog = logEvents.get(2);
        assertThat(successLog.getLevel()).isEqualTo(Level.INFO);
        assertThat(successLog.getFormattedMessage()).isEqualTo("Operation succeeded after 2 attempts");
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
     * - Should log 3 WARN messages for failed attempts
     * - Should log 1 ERROR message for final failure
     */
    @Test
    @DisplayName("unhappy path - when all retries exhausted - then log WARN for each failure and ERROR for final failure")
    void shouldLogAllFailedAttemptsAndFinalError() throws Exception {
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
                .isInstanceOf(FailedEventStorageException.class);

        // Then: four log events were reported: 3 WARNs (failures) + 1 ERROR (final failure)
        List<ILoggingEvent> logEvents = logAppender.list;
        assertThat(logEvents).hasSize(4);

        // First retry attempt failure (attempt 1): MinIO unavailable
        ILoggingEvent firstRetry = logEvents.getFirst();
        assertThat(firstRetry.getLevel()).isEqualTo(Level.WARN);
        assertThat(firstRetry.getFormattedMessage()).isEqualTo("Retry attempt 1/3 failed due to: MinIO unavailable");

        // Second retry attempt failure (attempt 2): MinIO unavailable
        ILoggingEvent secondRetry = logEvents.get(1);
        assertThat(secondRetry.getLevel()).isEqualTo(Level.WARN);
        assertThat(secondRetry.getFormattedMessage()).isEqualTo("Retry attempt 2/3 failed due to: MinIO unavailable");

        // Third retry attempt failure (attempt 3): MinIO unavailable
        ILoggingEvent thirdRetry = logEvents.get(2);
        assertThat(thirdRetry.getLevel()).isEqualTo(Level.WARN);
        assertThat(thirdRetry.getFormattedMessage()).isEqualTo("Retry attempt 3/3 failed due to: MinIO unavailable");

        // Final error after all retries exhausted
        ILoggingEvent errorLog = logEvents.get(3);
        assertThat(errorLog.getLevel()).isEqualTo(Level.ERROR);
        assertThat(errorLog.getFormattedMessage()).isEqualTo("Operation failed after 3 attempts due to: MinIO unavailable");
    }

    /**
     * Given:
     * - MinioClient succeeds on first attempt
     * <p>
     * When:
     * - save() is called
     * <p>
     * Then:
     * - Should log 1 INFO message for immediate success
     * - Should NOT log any WARN messages (no retries)
     */
    @Test
    @DisplayName("happy path - when operation succeeds immediately - then log INFO only without retry WARN logs")
    void shouldLogSuccessWithoutRetryWarnings() throws Exception {
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
        target.save(failedEvent);

        // Then: one log event was reported: 1 INFO (immediate success)
        List<ILoggingEvent> logEvents = logAppender.list;
        assertThat(logEvents).hasSize(1);

        // Immediate success on first attempt
        ILoggingEvent successLog = logEvents.getFirst();
        assertThat(successLog.getLevel()).isEqualTo(Level.INFO);
        assertThat(successLog.getFormattedMessage()).isEqualTo("Operation succeeded after 0 attempts");
    }

    @EnableRetry
    @Configuration
    static class TestConfig {

        @Bean
        public FailedEventGateway minioFailedEventGateway(
                MinioClient minioClient,
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
                    false
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
