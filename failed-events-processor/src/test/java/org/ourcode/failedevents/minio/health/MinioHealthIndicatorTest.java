package org.ourcode.failedevents.minio.health;

import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ourcode.failedevents.minio.configuration.MinioProperties;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MinioHealthIndicatorTest {

    @Mock
    private MinioClient minioClient;

    private MinioHealthIndicator target;

    private static final String TEST_ENDPOINT = "http://localhost:9000";
    private static final String TEST_BUCKET = "test-bucket";

    @BeforeEach
    void setUp() {
        MinioProperties minioProperties = new MinioProperties(
                TEST_ENDPOINT,
                "accessKey",
                "secretKey",
                TEST_BUCKET,
                true
        );

        target = new MinioHealthIndicator(minioClient, minioProperties);
    }

    @Test
    @DisplayName("happy path - when bucket exists - then return UP status")
    void shouldReturnUpStatusWhenBucketExists() throws Exception {
        // Given
        when(minioClient.bucketExists(any())).thenReturn(true);

        // When
        Health health = target.health();

        // Then
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("endpoint", TEST_ENDPOINT);
        assertThat(health.getDetails()).containsEntry("configuredBucket", TEST_BUCKET);
        assertThat(health.getDetails()).containsEntry("bucketExists", true);
        assertThat(health.getDetails()).doesNotContainKey("error");
    }

    @Test
    @DisplayName("unhappy path - when bucket does not exist - then return DOWN status")
    void shouldReturnDownStatusWhenBucketDoesNotExist() throws Exception {
        // Given
        when(minioClient.bucketExists(any())).thenReturn(false);

        // When
        Health health = target.health();

        // Then
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("endpoint", TEST_ENDPOINT);
        assertThat(health.getDetails()).containsEntry("configuredBucket", TEST_BUCKET);
        assertThat(health.getDetails()).containsEntry("bucketExists", false);
        assertThat(health.getDetails()).containsEntry("error", "Bucket '%s' does not exist".formatted(TEST_BUCKET));
    }

    @Test
    @DisplayName("error path - when MinIO is not reachable due to IOException - then return DOWN status")
    void shouldReturnDownStatusWhenMinioNotReachableDueToIOException() throws Exception {
        // Given
        IOException exception = new IOException("Connection refused");
        when(minioClient.bucketExists(any())).thenThrow(exception);

        // When
        Health health = target.health();

        // Then
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("endpoint", TEST_ENDPOINT);
        assertThat(health.getDetails()).containsEntry("configuredBucket", TEST_BUCKET);
        assertThat(health.getDetails()).containsEntry("error", "Connection refused");
        assertThat(health.getDetails()).containsEntry("errorType", "IOException");
    }

    @Test
    @DisplayName("error path - when exception message is null - then return DOWN status with unknown error message")
    void shouldReturnDownStatusWithUnknownErrorWhenExceptionMessageIsNull() throws Exception {
        // Given
        IOException exception = new IOException();
        when(minioClient.bucketExists(any())).thenThrow(exception);

        // When
        Health health = target.health();

        // Then
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("endpoint", TEST_ENDPOINT);
        assertThat(health.getDetails()).containsEntry("configuredBucket", TEST_BUCKET);
        assertThat(health.getDetails()).containsEntry("error", "unknown error: java.io.IOException");
        assertThat(health.getDetails()).containsEntry("errorType", "IOException");
    }

}
