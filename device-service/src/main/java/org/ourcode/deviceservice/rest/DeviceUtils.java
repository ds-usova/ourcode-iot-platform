package org.ourcode.deviceservice.rest;

import java.net.URI;

public class DeviceUtils {

    public static final String DEVICE_PATH = "/device";

    private DeviceUtils() {
        // private constructor to prevent instantiation
    }

    static org.ourcode.rest.model.Device toRestModel(org.ourcode.deviceservice.api.model.Device device) {
        if (device == null) {
            return null;
        }

        org.ourcode.rest.model.Device restDevice = new org.ourcode.rest.model.Device();
        restDevice.setId(device.id());
        restDevice.setType(device.type());
        restDevice.setMeta(device.metadata());
        return restDevice;
    }

    static org.ourcode.deviceservice.api.model.Device toApiModel(org.ourcode.rest.model.Device device) {
        if (device == null) {
            return null;
        }

        return new org.ourcode.deviceservice.api.model.Device(
                device.getId(),
                device.getType(),
                null,
                device.getMeta()
        );
    }

    static URI toUri(String deviceId) {
        return URI.create(DEVICE_PATH + "/" + deviceId);
    }

}
