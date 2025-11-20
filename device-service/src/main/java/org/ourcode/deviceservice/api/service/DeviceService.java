package org.ourcode.deviceservice.api.service;

import org.ourcode.deviceservice.api.model.Device;

public interface DeviceService {

    /**
     * Adds a new device.
     *
     * @param device The device to be added.
     * @return The added device.
     * @throws org.ourcode.deviceservice.api.exception.DuplicateException if a device with the same id already exists
     * @throws org.ourcode.deviceservice.api.exception.PersistenceException if an error occurs and the device cannot be persisted
     */
    Device addDevice(Device device);

    /**
     * Finds a device by its ID.
     * @param deviceId The ID of the device to be retrieved.
     * @return The device with the specified ID.
     * @throws org.ourcode.deviceservice.api.exception.NotFoundException if no device with the given ID exists
     */
    Device getDevice(String deviceId);

}
