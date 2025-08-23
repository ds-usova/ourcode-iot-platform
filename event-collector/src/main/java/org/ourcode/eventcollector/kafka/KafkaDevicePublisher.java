package org.ourcode.eventcollector.kafka;

import org.ourcode.eventcollector.api.gateway.DevicePublisher;
import org.ourcode.eventcollector.api.model.DeviceEvent;
import org.springframework.stereotype.Component;

@Component
public class KafkaDevicePublisher implements DevicePublisher {

    @Override
    public void publish(DeviceEvent deviceEvent) {

    }

}
