package org.ourcode.deviceservice.persistence.health;

import org.ourcode.deviceservice.persistence.configuration.FlywayProperties;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

@Component
public class ShardHealthIndicator implements HealthIndicator {

    private static final int CONNECTION_TIMEOUT_SECONDS = 5;
    private static final String TEST_QUERY = "SELECT 1";

    private final List<FlywayProperties.DataSource> dataSources;

    public ShardHealthIndicator(FlywayProperties flywayProperties) {
        this.dataSources = flywayProperties.dataSources();
    }

    @Override
    public Health health() {
        if (dataSources.isEmpty()) {
            return Health.down().withDetail("error", "No data sources configured").build();
        }

        Map<String, ShardStatus> details = dataSources.stream()
                .map(this::getShardHealthStatus)
                .collect(Collectors.toMap(ShardStatus::name, ShardStatus::withoutName));

        boolean allUp = details.values().stream().allMatch(ShardStatus::isUp);
        Health.Builder health = allUp ? Health.up() : Health.down();

        return health.withDetails(details).build();
    }

    private ShardStatus getShardHealthStatus(FlywayProperties.DataSource dataSource) {
        Properties properties = new Properties();
        properties.setProperty("user", dataSource.username());
        properties.setProperty("password", dataSource.password());
        properties.setProperty("connectTimeout", String.valueOf(CONNECTION_TIMEOUT_SECONDS * 1000));
        properties.setProperty("socketTimeout", String.valueOf(CONNECTION_TIMEOUT_SECONDS * 1000));

        try (Connection connection = DriverManager.getConnection(dataSource.url(), properties)) {
            try (PreparedStatement stmt = connection.prepareStatement(TEST_QUERY)) {
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return new ShardStatus(dataSource.name(), dataSource.url(), true, null);
                    } else {
                        return new ShardStatus(dataSource.name(), dataSource.url(), false, "Test query returned no results");
                    }
                }
            }
        } catch (SQLException e) {
            return new ShardStatus(dataSource.name(), dataSource.url(), false, e.getMessage());
        }
    }

    public record ShardStatus(
            String name,
            String url,
            boolean isUp,
            String errorMessage
    ) {

        public ShardStatus withoutName() {
            return new ShardStatus(null, url, isUp, errorMessage);
        }

    }

}
