package org.ourcode.eventcollector.api.gateway;

import org.ourcode.eventcollector.api.model.DeviceEvent;

public interface DevicePublisher {

    void publish(DeviceEvent deviceEvent);

}
