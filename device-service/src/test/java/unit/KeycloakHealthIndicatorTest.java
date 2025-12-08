package unit;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.ourcode.deviceservice.rest.health.KeycloakHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

@Slf4j
public class KeycloakHealthIndicatorTest {

    @Test
    @DisplayName("when api is not protected - health should be UP without error")
    public void testNotProtectedApi() {
        KeycloakHealthIndicator notProtectedIndicator = new KeycloakHealthIndicator("issuer-uri", false);

        Health health = notProtectedIndicator.health();
        log.debug("Health details (api not protected){}{}", System.lineSeparator(), health.getDetails());

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).isNotNull();

        assertThat(health.getDetails().get("url")).isNotNull();
        assertThat(health.getDetails().get("apiProtected")).isEqualTo(Boolean.FALSE);
        assertThat(health.getDetails().get("error")).isNull();
    }

    @Test
    @DisplayName("when keycloak returns bad request - health should be DOWN")
    void testKeycloakReturnsBadRequest() throws Exception {
        KeycloakHealthIndicator indicator = new KeycloakHealthIndicator("http://kc/realm", true);

        RestTemplate mockTemplate = Mockito.mock(RestTemplate.class);

        Field field = KeycloakHealthIndicator.class.getDeclaredField("restTemplate");
        field.setAccessible(true);
        field.set(indicator, mockTemplate);

        var response = new ResponseEntity<>("err", HttpStatus.BAD_REQUEST);

        Mockito.when(mockTemplate.getForEntity(anyString(), eq(String.class)))
               .thenReturn(response);

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).isNotNull();

        assertThat(health.getDetails().get("url")).isEqualTo("http://kc/realm/.well-known/openid-configuration");
        assertThat(health.getDetails().get("apiProtected")).isEqualTo(Boolean.TRUE);
        assertThat(health.getDetails().get("status")).isEqualTo(HttpStatus.BAD_REQUEST);
    }

}
