package org.ourcode.deviceservice.application;

import org.ourcode.deviceservice.api.exception.NotFoundException;
import org.ourcode.deviceservice.api.gateway.DeviceGateway;
import org.ourcode.deviceservice.api.model.Device;
import org.ourcode.deviceservice.api.service.DeviceService;
import org.springframework.stereotype.Service;

@Service
public class DeviceServiceImpl implements DeviceService {

    private final DeviceGateway deviceGateway;

    public DeviceServiceImpl(DeviceGateway deviceGateway) {
        this.deviceGateway = deviceGateway;
    }

    @Override
    public Device addDevice(Device device) {
        return deviceGateway.create(device);
    }

    @Override
    public Device getDevice(String deviceId) {
        return deviceGateway.getBy(deviceId)
                .orElseThrow(() -> new NotFoundException("Device with ID " + deviceId + " not found"));
    }

    @Override
    public Device updateDevice(String deviceId, String type, String meta) {
        return deviceGateway.update(deviceId, type, meta)
                .orElseThrow(() -> new NotFoundException("Device with ID " + deviceId + " not found"));
    }

}
