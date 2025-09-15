package org.ourcode.devicecollector.api.gateway;

import org.ourcode.devicecollector.api.model.Device;

import java.util.List;

public interface DeviceGateway {

    /**
     * Saves or updates a list of devices in batch.
     *
     * @param devices the list of devices to save
     */
    void upsertAll(List<Device> devices);

}
