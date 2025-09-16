package org.ourcode.devicecollector.persistence;

import lombok.extern.slf4j.Slf4j;
import org.ourcode.devicecollector.api.exception.PersistenceException;
import org.ourcode.devicecollector.api.gateway.DeviceGateway;
import org.ourcode.devicecollector.api.model.Device;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
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
