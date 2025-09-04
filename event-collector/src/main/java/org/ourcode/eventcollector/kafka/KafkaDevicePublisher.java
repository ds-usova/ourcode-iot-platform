package org.ourcode.eventcollector.kafka;

import com.ourcode.avro.Device;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.ourcode.eventcollector.api.exception.MessageNotPublishedException;
import org.ourcode.eventcollector.api.gateway.DevicePublisher;
import org.ourcode.eventcollector.api.gateway.DevicePublisherListener;
import org.ourcode.eventcollector.api.model.DeviceEvent;
import org.ourcode.eventcollector.kafka.configuration.KafkaTopics;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class KafkaDevicePublisher implements DevicePublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;

    private final List<DevicePublisherListener> listeners;

    public KafkaDevicePublisher(
            KafkaTemplate<String, Object> kafkaTemplate,
            KafkaTopics kafkaTopics,
            List<DevicePublisherListener> listeners
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = kafkaTopics.devices();
        this.listeners = listeners;
    }

    @Override
    public void publish(DeviceEvent deviceEvent) {
        log.debug("Publishing device with id={} to topic {}", deviceEvent.deviceId(), topic);

        String key = deviceEvent.deviceId();
        Device value = toAvroDevice(deviceEvent);

        try {
            ProducerRecord<String, Object> record = createProducerRecord(topic, key, value);

            kafkaTemplate.send(record)
                    .whenComplete((_, cause) -> {
                        if (cause != null) {
                            handleError(key, deviceEvent, cause);
                        }
                    })
                    .thenAccept(sendResult -> handleSuccess(sendResult, key));

        } catch (Exception e) {
            log.error("Failed to send message to topic: {}, key: {}", topic, deviceEvent.deviceId(), e);

            handleError(key, deviceEvent, e);
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

    private void handleError(String key, DeviceEvent deviceEvent, Throwable cause) {
        log.error("Failed to send message to topic: {}, key: {}", topic, key, cause);
        listeners.forEach(listener -> listener.onError(deviceEvent, cause));
    }

    private void handleSuccess(SendResult<String, Object> sendResult, String key) {
        log.debug("Device with id {} sent successfully to topic: {}, partition: {}, offset: {}",
                key,
                sendResult.getRecordMetadata().topic(),
                sendResult.getRecordMetadata().partition(),
                sendResult.getRecordMetadata().offset());
    }

}
