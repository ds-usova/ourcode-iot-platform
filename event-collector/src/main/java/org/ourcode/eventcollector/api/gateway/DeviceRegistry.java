package org.ourcode.eventcollector.api.gateway;

import org.ourcode.eventcollector.api.model.DeviceEvent;

public interface DeviceRegistry {

    /**
     * Check if device with given id exists in registry
     * If not exists, add it to registry and return true
     * @param deviceEvent device event
     * @return true if device was added, false if it already existed
     */
    boolean registerIfNotExists(DeviceEvent deviceEvent);

}
