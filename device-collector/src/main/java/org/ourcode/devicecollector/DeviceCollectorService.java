package org.ourcode.devicecollector;

import org.ourcode.devicecollector.kafka.configuration.KafkaTopics;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
        KafkaTopics.class
})
public class DeviceCollectorService {

    public static void main(String[] args) {
        SpringApplication.run(DeviceCollectorService.class, args);
    }

}
