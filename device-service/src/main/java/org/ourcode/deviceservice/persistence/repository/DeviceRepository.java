package org.ourcode.deviceservice.persistence.repository;

import org.ourcode.deviceservice.persistence.entity.DeviceEntity;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends CrudRepository<DeviceEntity, String> {

    @Override
    List<DeviceEntity> findAll();

    @Query(value = """
            INSERT INTO devices (device_id, device_type, created_at, meta)
            VALUES (:deviceId, :deviceType, :createdAt, :metadata)
            ON CONFLICT (device_id) DO NOTHING
            RETURNING device_id, device_type, created_at, meta
    """)
    Optional<DeviceEntity> save(String deviceId, String deviceType, Long createdAt, String metadata);

    @Query("""
        UPDATE devices
        SET
            device_type = COALESCE(:deviceType, device_type),
            meta        = COALESCE(:metadata, meta)
        WHERE device_id = :deviceId
        RETURNING device_id, device_type, created_at, meta
    """)
    Optional<DeviceEntity> updateDevice(String deviceId, String deviceType, String metadata);

}
