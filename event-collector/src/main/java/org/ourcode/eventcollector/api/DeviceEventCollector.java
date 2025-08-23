package org.ourcode.eventcollector.api;

import org.ourcode.eventcollector.api.model.DeviceEvent;

public interface DeviceEventCollector {

    void collect(DeviceEvent deviceEvent);

}
