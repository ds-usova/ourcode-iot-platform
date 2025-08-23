package org.ourcode.eventcollector.cache;

import org.ourcode.eventcollector.api.gateway.DeviceRegistry;
import org.ourcode.eventcollector.api.model.DeviceEvent;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryCacheDeviceRegistry implements DeviceRegistry {

    private final ConcurrentHashMap.KeySetView<String, Boolean> deviceIds = ConcurrentHashMap.newKeySet(100);

    @Override
    public boolean registerIfNotExists(DeviceEvent deviceEvent) {
        return deviceIds.add(deviceEvent.deviceId());
    }

}
