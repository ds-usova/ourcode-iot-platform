package org.ourcode.failedevents.api.gateway;

import org.ourcode.failedevents.api.model.FailedEvent;

public interface FailedEventGateway {

    void save(FailedEvent failedEvent);

}
