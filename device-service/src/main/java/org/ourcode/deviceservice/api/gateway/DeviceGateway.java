package org.ourcode.deviceservice.api.gateway;

import org.ourcode.deviceservice.api.model.Device;

import java.util.Optional;

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

    /**
     * Retrieves a device by its ID.
     *
     * @param deviceId the ID of the device to retrieve
     * @return an Optional containing the device if found, or empty if not found
     * @throws org.ourcode.deviceservice.api.exception.PersistenceException if the device cannot be retrieved
     */
    Optional<Device> getBy(String deviceId);

    /**
     * Updates an existing device.
     * @param deviceId the ID of the device to update
     * @param type the new type of the device, or null if not updating
     * @param meta the new metadata of the device, or null if not updating
     * @return an Optional containing the updated device if found, or empty if not found
     * @throws org.ourcode.deviceservice.api.exception.PersistenceException if the device cannot be updated
     */
    Optional<Device> update(String deviceId, String type, String meta);

}
