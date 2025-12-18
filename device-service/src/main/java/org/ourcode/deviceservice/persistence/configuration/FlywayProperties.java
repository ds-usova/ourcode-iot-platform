package org.ourcode.deviceservice.persistence.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "flyway")
public record FlywayProperties(
        String locations,
        List<DataSource> dataSources
) {

    public record DataSource(String name, String url, String username, String password) { }

}
