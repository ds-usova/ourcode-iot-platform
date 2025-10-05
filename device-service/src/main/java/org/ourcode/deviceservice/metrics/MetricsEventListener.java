package org.ourcode.deviceservice.metrics;

import org.ourcode.deviceservice.api.gateway.MetricsCollector;
import org.springframework.stereotype.Component;

@Component
public class MetricsEventListener {

    private final MetricsCollector metricsCollector;

    public MetricsEventListener(MetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;
    }


}
