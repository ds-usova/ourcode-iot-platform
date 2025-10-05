package org.ourcode.deviceservice.persistence;

import lombok.extern.slf4j.Slf4j;
import org.ourcode.deviceservice.api.exception.PersistenceException;
import org.ourcode.deviceservice.api.gateway.DeviceGateway;
import org.ourcode.deviceservice.api.model.Device;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class PostgresDeviceGateway implements DeviceGateway {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public PostgresDeviceGateway(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Retryable(
            retryFor = PersistenceException.class,
            maxAttemptsExpression = "${spring.retry.device-gateway.max-attempts}",
            backoff = @Backoff(delayExpression = "${spring.retry.device-gateway.backoff-delay}")
    )
    public void upsertAll(List<Device> devices) {
        log.debug("Upserting {} devices", devices.size());

        String sql = """
            INSERT INTO devices (device_id, device_type, created_at, meta)
            VALUES (:deviceId, :deviceType, :createdAt, :metadata)
            ON CONFLICT (device_id)
            DO UPDATE SET
                device_type = EXCLUDED.device_type,
                created_at  = EXCLUDED.created_at,
                meta        = EXCLUDED.meta
            """;

        SqlParameterSource[] batch = devices.stream()
                .map(device -> new MapSqlParameterSource()
                        .addValue("deviceId", device.id())
                        .addValue("deviceType", device.type())
                        .addValue("createdAt", device.timestamp())
                        .addValue("metadata", device.metadata()))
                .toArray(SqlParameterSource[]::new);

        try {
            jdbcTemplate.batchUpdate(sql, batch);
        } catch (DataAccessException e) {
            log.error("Error upserting devices", e);
            throw new PersistenceException(e.getMessage(), e);
        }
    }

}
