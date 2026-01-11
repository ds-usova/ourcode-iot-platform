package org.ourcode.devicecollector;

import org.ourcode.devicecollector.kafka.configuration.KafkaTopics;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.retry.annotation.EnableRetry;

@EnableRetry
@SpringBootApplication
@EnableConfigurationProperties({
        KafkaTopics.class,
})
public class FailedEventsProcessorApplication {

    public static void main(String[] args) {
        SpringApplication.run(FailedEventsProcessorApplication.class, args);
    }

}
