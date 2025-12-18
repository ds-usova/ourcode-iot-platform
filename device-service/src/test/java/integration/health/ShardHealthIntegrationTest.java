package integration.health;

import common.AbstractIntegrationTest;
import common.ToxiProxyUtils;
import common.containers.PostgresContainers;
import org.junit.jupiter.api.Test;
import org.ourcode.deviceservice.persistence.health.ShardHealthIndicator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;

import static common.containers.PostgresContainers.PROXY_0;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class ShardHealthIntegrationTest extends AbstractIntegrationTest {

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        AbstractIntegrationTest.setProperties(registry);
    }

    @Autowired
    private ShardHealthIndicator target;

    @Test
    public void testHealthy() {
        Health health = target.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).isNotNull();
    }

    @Test
    public void testUnhealthy() throws IOException {
        ToxiProxyUtils.cutConnection(PROXY_0);

        Health health = target.health();
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).isNotNull();

        if (health.getDetails().get("shard_0") instanceof ShardHealthIndicator.ShardStatus(
                String name, String url, boolean isUp, String errorMessage
        )) {
            assertThat(name).isNull();
            assertThat(isUp).isFalse();
            assertThat(url).isNotNull();
            assertThat(errorMessage).isNotNull();
        } else {
            fail("There is no detail for shard_0");
        }

        PostgresContainers.resetProxies();
    }

}
