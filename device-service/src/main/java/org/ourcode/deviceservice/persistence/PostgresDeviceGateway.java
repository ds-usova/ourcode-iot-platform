package org.ourcode.deviceservice.persistence;

import lombok.extern.slf4j.Slf4j;
import org.ourcode.deviceservice.api.exception.DuplicateException;
import org.ourcode.deviceservice.api.gateway.DeviceGateway;
import org.ourcode.deviceservice.api.model.Device;
import org.ourcode.deviceservice.persistence.configuration.TranslatePersistenceExceptions;
import org.ourcode.deviceservice.util.TimeUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@TranslatePersistenceExceptions
public class PostgresDeviceGateway implements DeviceGateway {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public PostgresDeviceGateway(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Retryable(
            retryFor = DataAccessException.class,
            maxAttemptsExpression = "${spring.retry.device-gateway.max-attempts}",
            backoff = @Backoff(delayExpression = "${spring.retry.device-gateway.backoff-delay}")
    )
    @Transactional
    public Device create(Device device) {
        log.debug("Creating device with ID {}", device.id());

        String sql = """
                    INSERT INTO devices (device_id, device_type, created_at, meta)
                    VALUES (:deviceId, :deviceType, :createdAt, :metadata)
                    ON CONFLICT (device_id) DO NOTHING
                    RETURNING device_id, device_type, created_at, meta
                """;

        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("deviceId", device.id())
                .addValue("deviceType", device.type())
                .addValue("createdAt", TimeUtils.now())
                .addValue("metadata", device.metadata());

        List<Device> results = jdbcTemplate.query(sql, params, (rs, rowNum) ->
                new Device(
                        rs.getString("device_id"),
                        rs.getString("device_type"),
                        rs.getLong("created_at"),
                        rs.getString("meta")
                )
        );

        if (results.isEmpty()) {
            throw new DuplicateException("Device with ID " + device.id() + " already exists");
        }

        return results.getFirst();
    }

}
