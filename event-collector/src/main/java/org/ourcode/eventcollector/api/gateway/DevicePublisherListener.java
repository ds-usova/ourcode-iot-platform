package org.ourcode.eventcollector.api.gateway;

import org.ourcode.eventcollector.api.model.DeviceEvent;

public interface DevicePublisherListener {

    void onError(DeviceEvent deviceEvent, Throwable cause);

}
