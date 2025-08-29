package org.ourcode.eventcollector.kafka;

import com.ourcode.avro.Device;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.ourcode.eventcollector.api.exception.MessageNotPublishedException;
import org.ourcode.eventcollector.api.gateway.DevicePublisher;
import org.ourcode.eventcollector.api.model.DeviceEvent;
import org.ourcode.eventcollector.kafka.configuration.KafkaTopics;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KafkaDevicePublisher implements DevicePublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;

    public KafkaDevicePublisher(
            KafkaTemplate<String, Object> kafkaTemplate,
            KafkaTopics kafkaTopics
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = kafkaTopics.devices();
    }

    @Override
    public void publish(DeviceEvent deviceEvent) {
        log.debug("Publishing device with id={}", deviceEvent.deviceId());

        try {
            String key = deviceEvent.deviceId();
            Device message = toAvroDevice(deviceEvent);

            ProducerRecord<String, Object> record = createProducerRecord(topic, key, message);
            SendResult<String, Object> result = kafkaTemplate.send(record).get();

            log.debug("Message sent successfully to topic: {}, partition: {}, offset: {}",
                    result.getRecordMetadata().topic(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());

        } catch (Exception e) {
            log.error("Failed to send message to topic: {}, key: {}", topic, deviceEvent.deviceId(), e);
            throw new MessageNotPublishedException("Failed to send message", e);
        }
    }

    private ProducerRecord<String, Object> createProducerRecord(String topic, String key, Object message) {
        return new ProducerRecord<>(topic, null, key, message);
    }

    private Device toAvroDevice(DeviceEvent device) {
        return Device.newBuilder()
                .setDeviceId(device.deviceId())
                .build();
    }

}
