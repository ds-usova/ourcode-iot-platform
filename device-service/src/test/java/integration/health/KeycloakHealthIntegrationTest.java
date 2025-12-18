package integration.health;

import common.AbstractIntegrationTest;
import common.containers.KeycloakContainer;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.ourcode.deviceservice.rest.health.KeycloakHealthIndicator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class KeycloakHealthIntegrationTest extends AbstractIntegrationTest {

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        AbstractIntegrationTest.setProperties(registry);
        registry.add("app.apiProtected", () -> "true");
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
                () -> KeycloakContainer.getAuthServerUrl() + "/realms/our-code");
    }

    @Autowired
    private KeycloakHealthIndicator target;

    @Test
    public void testHealthy() {
        Health health = target.health();
        log.debug("Health details (must be up){}{}", System.lineSeparator(), health.getDetails());

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).isNotNull();

        assertThat(health.getDetails().get("url")).isNotNull();
        assertThat(health.getDetails().get("apiProtected")).isEqualTo(Boolean.TRUE);
    }

    @Test
    public void testUnhealthy() {
        KeycloakContainer.cutConnection();

        Health health = target.health();
        log.debug("Health details (must be down){}{}", System.lineSeparator(), health.getDetails());

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).isNotNull();

        assertThat(health.getDetails().get("url")).isNotNull();
        assertThat(health.getDetails().get("apiProtected")).isEqualTo(Boolean.TRUE);
        assertThat(health.getDetails().get("error")).isNotNull();
    }

}
