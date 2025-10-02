package org.ourcode.devicecollector.persistence.repository;

import org.ourcode.devicecollector.persistence.entity.DeviceEntity;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface DeviceRepository extends CrudRepository<DeviceEntity, String> {

    @Override
    List<DeviceEntity> findAll();

}
