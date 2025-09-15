package org.ourcode.devicecollector.application;

import org.ourcode.devicecollector.api.gateway.DeviceGateway;
import org.ourcode.devicecollector.api.model.Device;
import org.ourcode.devicecollector.api.service.DeviceCollector;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DeviceCollectorImpl implements DeviceCollector {

    private final DeviceGateway deviceGateway;

    public DeviceCollectorImpl(DeviceGateway deviceGateway) {
        this.deviceGateway = deviceGateway;
    }

    @Override
    public void collectDevices(List<Device> devices) {
        deviceGateway.upsertAll(devices);
    }

}
