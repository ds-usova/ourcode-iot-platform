package org.ourcode.deviceservice.persistence.configuration;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayConfiguration {

    @Bean
    public FlywayMigrationInitializer flywayMigrationInitializer(FlywayProperties flywayProperties) {
        return new FlywayMigrationInitializer(Flyway.configure().load(), x -> {
            for (FlywayProperties.DataSource source : flywayProperties.dataSources()) {
                Flyway.configure()
                        .dataSource(source.url(), source.username(), source.password())
                        .locations(flywayProperties.locations())
                        .load()
                        .migrate();
            }
        });
    }

}
