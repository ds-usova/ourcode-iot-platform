package org.ourcode.devicecollector.api.gateway;

import org.ourcode.devicecollector.api.model.Device;

public interface DeviceGateway {

    /**
     * Saves a new device or updates the existing one.
     *
     * @param device the device to save
     * @return updated device
     */
    Device upsert(Device device);

}
