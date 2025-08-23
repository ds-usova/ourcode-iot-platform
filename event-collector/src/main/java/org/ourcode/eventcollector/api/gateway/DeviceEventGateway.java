package org.ourcode.eventcollector.api.gateway;

import org.ourcode.eventcollector.api.model.DeviceEvent;

public interface DeviceEventGateway {

    DeviceEvent save(DeviceEvent event);

}
