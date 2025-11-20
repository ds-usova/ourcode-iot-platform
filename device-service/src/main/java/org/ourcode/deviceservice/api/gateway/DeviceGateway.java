package org.ourcode.deviceservice.api.gateway;

import org.ourcode.deviceservice.api.model.Device;

public interface DeviceGateway {

    /**
     * Creates a new device.
     *
     * @param device the device to create
     * @return the created device
     * @throws org.ourcode.deviceservice.api.exception.DuplicateException   if the device already exists
     * @throws org.ourcode.deviceservice.api.exception.PersistenceException if the device cannot be created
     */
    Device create(Device device);

}
