package org.ourcode.devicecollector;

import org.ourcode.devicecollector.kafka.configuration.KafkaTopics;
import org.ourcode.devicecollector.persistence.configuration.FlywayProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
        KafkaTopics.class,
        FlywayProperties.class
})
public class DeviceCollectorApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeviceCollectorApplication.class, args);
    }

}
