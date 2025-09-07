package org.ourcode.devicecollector.persistence.configuration;

import org.apache.shardingsphere.driver.api.yaml.YamlShardingSphereDataSourceFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;

@Configuration
public class DataSourceConfiguration {

    @Bean
    public DataSource dataSource() throws SQLException, IOException {
        ClassPathResource resource = new ClassPathResource("db/sharding.yaml");
        return YamlShardingSphereDataSourceFactory.createDataSource(resource.getFile());
    }

}
