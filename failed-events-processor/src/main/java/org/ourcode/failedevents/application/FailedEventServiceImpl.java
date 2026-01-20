package org.ourcode.failedevents.application;

import lombok.extern.slf4j.Slf4j;
import org.ourcode.failedevents.api.gateway.FailedEventGateway;
import org.ourcode.failedevents.api.model.FailedEvent;
import org.ourcode.failedevents.api.service.FailedEventService;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FailedEventServiceImpl implements FailedEventService {

    private final FailedEventGateway failedEventGateway;

    public FailedEventServiceImpl(FailedEventGateway failedEventGateway) {
        this.failedEventGateway = failedEventGateway;
    }

    @Override
    public void save(FailedEvent failedEvent) {
        log.debug("Saving failed event: {}", failedEvent);
        failedEventGateway.save(failedEvent);
    }

}
