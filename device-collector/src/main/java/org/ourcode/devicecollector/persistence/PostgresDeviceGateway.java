package org.ourcode.devicecollector.persistence;

import org.ourcode.devicecollector.api.gateway.DeviceGateway;
import org.ourcode.devicecollector.api.model.Device;
import org.ourcode.devicecollector.persistence.repository.DeviceRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PostgresDeviceGateway implements DeviceGateway {

    private final DeviceRepository deviceRepository;

    public PostgresDeviceGateway(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    @Override
    @Transactional
    public Device upsert(Device device) {
        return deviceRepository.upsert(
                device.id(),
                device.type(),
                device.timestamp(),
                device.metadata()
        ).toModel();
    }

}
