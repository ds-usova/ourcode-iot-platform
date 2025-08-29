package org.ourcode.eventcollector.cache;

import lombok.extern.slf4j.Slf4j;
import org.ourcode.eventcollector.api.gateway.DeviceRegistry;
import org.ourcode.eventcollector.api.model.DeviceEvent;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class InMemoryCacheDeviceRegistry implements DeviceRegistry {

    private final ConcurrentHashMap.KeySetView<String, Boolean> deviceIds = ConcurrentHashMap.newKeySet(100);

    @Override
    public boolean registerIfNotExists(DeviceEvent deviceEvent) {
        log.debug("Registering device ID: {}", deviceEvent.deviceId());
        return deviceIds.add(deviceEvent.deviceId());
    }

    @Override
    public void unregister(DeviceEvent deviceEvent) {
        deviceIds.remove(deviceEvent.deviceId());
    }

}
