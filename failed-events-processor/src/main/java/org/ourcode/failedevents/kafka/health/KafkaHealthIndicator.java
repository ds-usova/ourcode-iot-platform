package org.ourcode.failedevents.kafka.health;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class KafkaHealthIndicator implements HealthIndicator {

    private final KafkaAdmin kafkaAdmin;

    public KafkaHealthIndicator(KafkaAdmin kafkaAdmin) {
        this.kafkaAdmin = kafkaAdmin;
    }

    @Override
    public Health health() {
        log.debug("Health check: querying Kafka topics");

        try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            Set<String> topics = adminClient.listTopics(new ListTopicsOptions().timeoutMs(5000))
                    .names()
                    .get(5, TimeUnit.SECONDS);

            return Health.up()
                    .withDetail("topics", topics)
                    .build();
        } catch (Exception e) {
            log.error("Health check: Kafka is not reachable", e);

            return Health.down()
                    .withDetail("error", e.getMessage() == null ? "unknown error: " + e.getClass() : e.getMessage())
                    .build();
        }
    }
}
