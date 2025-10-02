package org.ourcode.devicecollector.application;

import org.ourcode.devicecollector.api.events.DevicesProcessedEvent;
import org.ourcode.devicecollector.api.gateway.DeviceGateway;
import org.ourcode.devicecollector.api.model.Device;
import org.ourcode.devicecollector.api.service.DeviceCollector;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DeviceCollectorImpl implements DeviceCollector {

    private final DeviceGateway deviceGateway;
    private final ApplicationEventPublisher publisher;

    public DeviceCollectorImpl(DeviceGateway deviceGateway, ApplicationEventPublisher publisher) {
        this.deviceGateway = deviceGateway;
        this.publisher = publisher;
    }

    @Override
    public void collectDevices(List<Device> devices) {
        deviceGateway.upsertAll(devices);
        publisher.publishEvent(new DevicesProcessedEvent(devices.size()));
    }

}
