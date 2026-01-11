package org.ourcode.devicecollector.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.ourcode.devicecollector.api.gateway.MetricsCollector;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MicrometerMetricsCollector implements MetricsCollector {

    private final Counter deviceProcessingError;
    private final Counter invalidDeviceCount;
    private final Counter successfulDevicesCounter;

    public MicrometerMetricsCollector(MeterRegistry meterRegistry) {
        this.deviceProcessingError = Counter.builder("devicecollector.devices.errors")
                .description("Number of device processing errors")
                .register(meterRegistry);

        this.invalidDeviceCount = Counter.builder("devicecollector.devices.invalid")
                .description("Number of invalid device inputs")
                .register(meterRegistry);

        this.successfulDevicesCounter = Counter.builder("devicecollector.devices.successful")
                .description("Number of successful device updates")
                .register(meterRegistry);
    }

    @Override
    public void incrementDeviceProcessingErrors(int count) {
        runSafe(() -> deviceProcessingError.increment(count));
    }

    @Override
    public void incrementInvalidDeviceInput(int count) {
        runSafe(() -> invalidDeviceCount.increment(count));
    }

    @Override
    public void incrementSuccessfulDevices(int count) {
        runSafe(() -> successfulDevicesCounter.increment(count));
    }

    private void runSafe(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            log.error("Error while updating metrics", e);
        }
    }

}
