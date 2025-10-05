package system_test;

import common.AbstractIntegrationTest;
import common.ToxiProxyUtils;
import common.containers.PostgresContainers;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;

import static common.containers.PostgresContainers.PROXY_0;
import static common.containers.PostgresContainers.PROXY_1;

@Slf4j
public class ToxicTest extends AbstractIntegrationTest {

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        AbstractIntegrationTest.setProperties(registry);
        // Set low retry attempts and backoff delay for faster tests
        registry.add("spring.retry.device-gateway.max-attempts", () -> 2);
        registry.add("spring.retry.device-gateway.backoff-delay", () -> 100);
    }

    /**
     * Given:
     * - Both shards of PostgreSQL are not available (simulated using Toxiproxy)
     * <p>
     * When:
     * - todo
     * <p>
     * Then:
     * - todo
     */
    @Test
    @DisplayName("Toxic postgres: todo")
    void todo() throws IOException {
        ToxiProxyUtils.cutConnection(PROXY_0);
        ToxiProxyUtils.cutConnection(PROXY_1);

        // todo: example

        PostgresContainers.resetProxies();
    }

}
