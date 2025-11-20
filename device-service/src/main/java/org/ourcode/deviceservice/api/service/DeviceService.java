package org.ourcode.deviceservice.api.service;

import org.ourcode.deviceservice.api.model.Device;

public interface DeviceService {

    /**
     * Adds a new device.
     *
     * @param device The device to be added.
     * @return The added device.
     * @throws org.ourcode.deviceservice.api.exception.DuplicateException if a device with the same id already exists.
     */
    Device addDevice(Device device);

}
