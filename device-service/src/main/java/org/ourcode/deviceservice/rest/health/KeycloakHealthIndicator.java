package org.ourcode.deviceservice.rest.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class KeycloakHealthIndicator implements HealthIndicator {

    private static final String DISCOVERY_PATH = "/.well-known/openid-configuration";
    private final String healthUrl;
    private final RestTemplate restTemplate;
    private final boolean apiProtected;

    public KeycloakHealthIndicator(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUrl,
            @Value("${app.apiProtected:true}") boolean apiProtected
    ) {
        this.healthUrl = issuerUrl + DISCOVERY_PATH;
        this.restTemplate = new RestTemplate();
        this.apiProtected = apiProtected;
    }

    @Override
    public Health health() {
        if (!apiProtected) {
            return Health.up().withDetail("url", healthUrl).withDetail("apiProtected", false).build();
        }

        try {
            var response = restTemplate.getForEntity(healthUrl, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return Health.up()
                        .withDetail("url", healthUrl)
                        .withDetail("apiProtected", true)
                        .build();
            }

            log.debug("Keycloak health check returned non-2xx status: {}", response.getStatusCode());
            return Health.down()
                    .withDetail("url", healthUrl)
                    .withDetail("status", response.getStatusCode())
                    .withDetail("apiProtected", true)
                    .build();
        } catch (Exception e) {
            log.debug("Keycloak health check failed: {}", e.getMessage());
            return Health.down()
                    .withDetail("url", healthUrl)
                    .withDetail("apiProtected", true)
                    .withException(e)
                    .build();
        }
    }

}
