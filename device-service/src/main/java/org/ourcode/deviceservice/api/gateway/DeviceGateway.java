package org.ourcode.deviceservice.api.gateway;

import org.ourcode.deviceservice.api.model.Device;

import java.util.List;

public interface DeviceGateway {

    /**
     * Saves or updates a list of devices in batch.
     *
     * @param devices the list of devices to save
     * @throws org.ourcode.deviceservice.api.exception.PersistenceException if devices cannot be saved
     */
    void upsertAll(List<Device> devices);

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
