package org.ourcode.devicecollector.persistence.repository;

import org.ourcode.devicecollector.persistence.entity.DeviceEntity;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

public interface DeviceRepository extends CrudRepository<DeviceEntity, String> {

    @Query("""
        INSERT INTO devices (device_id, device_type, created_at, meta)
        VALUES (:id, :type, :createdAt, :metadata)
        ON CONFLICT (device_id)
        DO UPDATE SET
            device_type = EXCLUDED.device_type,
            created_at  = EXCLUDED.created_at,
            meta        = EXCLUDED.meta
        RETURNING *
    """)
    DeviceEntity upsert(String id, String type, long createdAt, String metadata);

}
