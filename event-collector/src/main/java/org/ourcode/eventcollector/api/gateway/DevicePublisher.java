package org.ourcode.eventcollector.api.gateway;

import org.ourcode.eventcollector.api.model.DeviceEvent;

public interface DevicePublisher {

    /**
     * Publish a new device
     * @param deviceEvent the device to publish
     * @throws org.ourcode.eventcollector.api.exception.MessageNotPublishedException if the message could not be published
     */
    void publish(DeviceEvent deviceEvent);

}
