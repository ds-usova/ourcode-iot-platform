package org.ourcode.eventcollector.cache;

import org.ourcode.eventcollector.api.gateway.DevicePublisherListener;
import org.ourcode.eventcollector.api.model.DeviceEvent;
import org.springframework.stereotype.Component;

@Component
public class CacheDevicePublisherListener implements DevicePublisherListener {

    private final InMemoryCacheDeviceRegistry registry;

    public CacheDevicePublisherListener(InMemoryCacheDeviceRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void onError(DeviceEvent deviceEvent, Throwable cause) {
        registry.unregister(deviceEvent);
    }

}
