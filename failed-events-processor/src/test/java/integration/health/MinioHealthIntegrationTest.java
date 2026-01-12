package integration.health;

import common.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.ourcode.failedevents.minio.health.MinioHealthIndicator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

public class MinioHealthIntegrationTest extends AbstractIntegrationTest {

    private static final String BUCKET_NAME = "test-bucket";

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        AbstractIntegrationTest.setProperties(registry);

        registry.add("app.minio.bucket", () -> BUCKET_NAME);
        registry.add("app.minio.auto-create-bucket", () -> true);
    }

    @Autowired
    private MinioHealthIndicator target;

    @Test
    public void testHealthy() {
        Health health = target.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).isNotNull();

        assertThat(health.getDetails().get("endpoint")).isNotNull();
        assertThat(health.getDetails().get("configuredBucket")).isEqualTo(BUCKET_NAME);
        assertThat(health.getDetails().get("bucketExists")).isEqualTo(true);
    }

}
