package org.ourcode.eventcollector.api.service;

import org.ourcode.eventcollector.api.model.DeviceEvent;

public interface DeviceEventCollector {

    void collect(DeviceEvent deviceEvent);

}
