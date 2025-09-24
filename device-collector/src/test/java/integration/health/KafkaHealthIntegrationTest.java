package integration.health;

import common.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.ourcode.devicecollector.kafka.configuration.KafkaTopics;
import org.ourcode.devicecollector.kafka.health.KafkaHealthIndicator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class KafkaHealthIntegrationTest extends AbstractIntegrationTest {

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        AbstractIntegrationTest.setProperties(registry);
    }

    @Autowired
    private KafkaHealthIndicator target;

    @Autowired
    private KafkaTopics kafkaTopics;

    @Test
    public void testHealthy() {
        Health health = target.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).isNotNull();

        assertThat(health.getDetails().get("topics")).isNotNull();
        assertThat(health.getDetails().get("topics")).isInstanceOf(Set.class);

        Set<String> topics = (Set<String>) health.getDetails().get("topics");
        assertThat(topics).contains(kafkaTopics.devices());
    }

}
