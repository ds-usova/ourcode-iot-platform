package org.ourcode.devicecollector.kafka;

import com.ourcode.avro.Device;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.ourcode.devicecollector.api.service.DeviceCollector;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class DeviceKafkaConsumer {

    private final DeviceCollector deviceCollector;

    public DeviceKafkaConsumer(DeviceCollector deviceCollector) {
        this.deviceCollector = deviceCollector;
    }

    @KafkaListener(topics = "${app.kafka.topics.devices}", groupId = "${spring.kafka.consumer.group-id}")
    public void listen(
            List<ConsumerRecord<String, Device>> records,
            Acknowledgment ack
    ) {
        log.debug("Received {} device records", records.size());

        List<org.ourcode.devicecollector.api.model.Device> devices = records.stream()
                .map(it -> toModel(it.value()))
                .collect(Collectors.toList());

        deviceCollector.collectDevices(devices);

        ack.acknowledge();
    }

    private org.ourcode.devicecollector.api.model.Device toModel(Device device) {
        return new org.ourcode.devicecollector.api.model.Device(
                device.getDeviceId(),
                device.getDeviceType(),
                device.getCreatedAt(),
                device.getMeta()
        );
    }

}
