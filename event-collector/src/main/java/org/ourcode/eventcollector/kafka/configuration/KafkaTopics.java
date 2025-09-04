package org.ourcode.eventcollector.kafka.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka.topics")
public record KafkaTopics(
        String events,
        String devices
) { }