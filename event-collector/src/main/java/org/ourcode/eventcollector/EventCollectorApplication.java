package org.ourcode.eventcollector;

import org.ourcode.eventcollector.kafka.configuration.KafkaTopics;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
        KafkaTopics.class
})
public class EventCollectorApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventCollectorApplication.class, args);
    }

}
