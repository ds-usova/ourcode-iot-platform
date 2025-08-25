package org.ourcode.eventcollector.service;

import lombok.extern.slf4j.Slf4j;
import org.ourcode.eventcollector.api.DeviceEventCollector;
import org.ourcode.eventcollector.api.gateway.DeviceEventGateway;
import org.ourcode.eventcollector.api.gateway.DeviceRegistry;
import org.ourcode.eventcollector.api.gateway.DevicePublisher;
import org.ourcode.eventcollector.api.model.DeviceEvent;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DeviceEventCollectorImpl implements DeviceEventCollector {

    private final DeviceEventGateway deviceEventGateway;
    private final DeviceRegistry deviceRegistry;
    private final DevicePublisher devicePublisher;

    public DeviceEventCollectorImpl(DeviceEventGateway deviceEventGateway,
                                    DeviceRegistry deviceRegistry,
                                    DevicePublisher devicePublisher
    ) {
        this.deviceEventGateway = deviceEventGateway;
        this.deviceRegistry = deviceRegistry;
        this.devicePublisher = devicePublisher;
    }

    @Override
    public void collect(DeviceEvent deviceEvent) {
        log.debug("Collecting device event: {}", deviceEvent);

        deviceEventGateway.save(deviceEvent);
        if (deviceRegistry.registerIfNotExists(deviceEvent)) {
            devicePublisher.publish(deviceEvent);
        }
    }

}
