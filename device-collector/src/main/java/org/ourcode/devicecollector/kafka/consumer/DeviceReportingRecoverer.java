package org.ourcode.devicecollector.kafka.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.ourcode.avro.Device;
import org.ourcode.devicecollector.api.events.UpdateDeviceFailure;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;

public class DeviceReportingRecoverer implements ConsumerRecordRecoverer {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final ConsumerRecordRecoverer delegate;

    public DeviceReportingRecoverer(ApplicationEventPublisher applicationEventPublisher,
                                    ConsumerRecordRecoverer delegate) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.delegate = delegate;
    }

    @Override
    public void accept(ConsumerRecord<?, ?> record, Exception exception) {
        delegate.accept(record, exception);

        UpdateDeviceFailure event = switch (record.value()) {
          case Device _ -> new UpdateDeviceFailure(UpdateDeviceFailure.Reason.PROCESSING_ERROR, 1);
          case null, default -> new UpdateDeviceFailure(UpdateDeviceFailure.Reason.INVALID_INPUT, 1);
        };

        applicationEventPublisher.publishEvent(event);
    }

}
