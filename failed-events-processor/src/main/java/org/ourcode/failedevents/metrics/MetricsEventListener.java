package org.ourcode.failedevents.metrics;

import org.ourcode.failedevents.api.events.DevicesProcessedEvent;
import org.ourcode.failedevents.api.events.UpdateDeviceFailure;
import org.ourcode.failedevents.api.gateway.MetricsCollector;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class MetricsEventListener {

    private final MetricsCollector metricsCollector;

    public MetricsEventListener(MetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;
    }

    @EventListener
    public void handleUpdateDeviceFailure(UpdateDeviceFailure event) {
        switch (event.reason()) {
            case INVALID_INPUT -> metricsCollector.incrementInvalidDeviceInput(event.count());
            case PROCESSING_ERROR -> metricsCollector.incrementDeviceProcessingErrors(event.count());
        }
    }

    @EventListener
    public void handleDeviceProcessed(DevicesProcessedEvent event) {
        metricsCollector.incrementSuccessfulDevices(event.count());
    }

}
