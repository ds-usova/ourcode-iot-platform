package org.ourcode.devicecollector.api.service;

import org.ourcode.devicecollector.api.model.Device;

import java.util.List;

public interface DeviceCollector {

    /**
     * Collects a list of devices and processes them accordingly.
     *
     * @param devices the list of devices to be collected
     */
    void collectDevices(List<Device> devices);

}
