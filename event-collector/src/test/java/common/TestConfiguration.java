package common;

import org.ourcode.eventcollector.cassandra.DeviceEventRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestConfiguration {

    @Bean
    public DatabaseManager databaseManager(DeviceEventRepository deviceEventRepository) {
        return new DatabaseManager(deviceEventRepository);
    }

}
