package org.ourcode.failedevents.minio.health;

import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.ourcode.failedevents.minio.MinioProperties;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MinioHealthIndicator implements HealthIndicator {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    public MinioHealthIndicator(MinioClient minioClient, MinioProperties minioProperties) {
        this.minioClient = minioClient;
        this.minioProperties = minioProperties;
    }

    @Override
    public Health health() {
        log.debug("Health check: checking MinIO bucket {} existence", minioProperties.bucket());

        try {
            boolean bucketExists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(minioProperties.bucket())
                            .build()
            );

            if (!bucketExists) {
                log.warn("Health check: configured MinIO bucket does not exist: {}", minioProperties.bucket());
                return Health.down()
                        .withDetail("endpoint", minioProperties.endpoint())
                        .withDetail("configuredBucket", minioProperties.bucket())
                        .withDetail("bucketExists", false)
                        .withDetail("error", "Bucket '" + minioProperties.bucket() + "' does not exist")
                        .build();
            }

            return Health.up()
                    .withDetail("endpoint", minioProperties.endpoint())
                    .withDetail("configuredBucket", minioProperties.bucket())
                    .withDetail("bucketExists", true)
                    .build();
        } catch (Exception e) {
            log.error("Health check: MinIO is not reachable", e);

            return Health.down()
                    .withDetail("endpoint", minioProperties.endpoint())
                    .withDetail("configuredBucket", minioProperties.bucket())
                    .withDetail("error", e.getMessage() == null ? "unknown error: " + e.getClass().getName() : e.getMessage())
                    .withDetail("errorType", e.getClass().getSimpleName())
                    .build();
        }
    }
}
