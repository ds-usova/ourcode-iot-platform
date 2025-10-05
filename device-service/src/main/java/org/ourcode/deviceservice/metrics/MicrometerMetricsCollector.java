package org.ourcode.deviceservice.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.ourcode.deviceservice.api.gateway.MetricsCollector;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MicrometerMetricsCollector implements MetricsCollector {

    private final Counter todo;

    public MicrometerMetricsCollector(MeterRegistry meterRegistry) {
        this.todo = Counter.builder("deviceservice.todo")
                .description("Todo metric")
                .register(meterRegistry);
    }

    private void runSafe(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            log.error("Error while updating metrics", e);
        }
    }

}
