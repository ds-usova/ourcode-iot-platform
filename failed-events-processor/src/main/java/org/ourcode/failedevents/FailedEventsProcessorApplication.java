package org.ourcode.failedevents;

import org.ourcode.failedevents.kafka.configuration.KafkaTopics;
import org.ourcode.failedevents.minio.MinioProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.retry.annotation.EnableRetry;

@EnableRetry
@SpringBootApplication
@EnableConfigurationProperties({
        KafkaTopics.class,
        MinioProperties.class
})
public class FailedEventsProcessorApplication {

    public static void main(String[] args) {
        SpringApplication.run(FailedEventsProcessorApplication.class, args);
    }

}
