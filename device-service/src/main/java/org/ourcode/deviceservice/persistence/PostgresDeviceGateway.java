package org.ourcode.deviceservice.persistence;

import lombok.extern.slf4j.Slf4j;
import org.ourcode.deviceservice.api.exception.DuplicateException;
import org.ourcode.deviceservice.api.gateway.DeviceGateway;
import org.ourcode.deviceservice.api.model.Device;
import org.ourcode.deviceservice.persistence.configuration.TranslatePersistenceExceptions;
import org.ourcode.deviceservice.persistence.entity.DeviceEntity;
import org.ourcode.deviceservice.persistence.repository.DeviceRepository;
import org.ourcode.deviceservice.util.TimeUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Component
@TranslatePersistenceExceptions
public class PostgresDeviceGateway implements DeviceGateway {

    private final DeviceRepository deviceRepository;

    public PostgresDeviceGateway(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
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

        return deviceRepository.save(device.id(), device.type(), TimeUtils.now(), device.metadata())
                .map(DeviceEntity::toModel)
                .orElseThrow(() -> new DuplicateException("Device with ID " + device.id() + " already exists"));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Device> getBy(String deviceId) {
        log.debug("Getting device with ID {}", deviceId);
        return deviceRepository.findById(deviceId).map(DeviceEntity::toModel);
    }

    @Override
    @Transactional
    public Optional<Device> update(String deviceId, String type, String meta) {
        log.debug("Updating device with ID {}", deviceId);
        return deviceRepository.updateDevice(deviceId, type, meta).map(DeviceEntity::toModel);
    }

}
