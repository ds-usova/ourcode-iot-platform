package org.ourcode.eventcollector.cassandra;

import lombok.extern.slf4j.Slf4j;
import org.ourcode.eventcollector.api.gateway.DeviceEventGateway;
import org.ourcode.eventcollector.api.model.DeviceEvent;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CassandraDeviceEventGateway implements DeviceEventGateway {

    private final DeviceEventRepository repository;

    public CassandraDeviceEventGateway(DeviceEventRepository repository) {
        this.repository = repository;
    }

    @Override
    public DeviceEvent save(DeviceEvent event) {
        log.debug("Saving device event: {}", event);

        return repository.save(DeviceEventEntity.from(event)).toModel();
    }

}
