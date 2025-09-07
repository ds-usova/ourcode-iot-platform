package org.ourcode.devicecollector.kafka;

import com.ourcode.avro.Device;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DeviceKafkaConsumer {

    @KafkaListener(topics = "${app.kafka.topics.devices}", groupId = "${spring.kafka.consumer.group-id}")
    public void listen(
            @Header(KafkaHeaders.RECEIVED_TOPIC) String receivedTopic,
            ConsumerRecord<String, Device> record,
            Acknowledgment ack
    ) {
        log.debug("Received device: key = {}, value = {}, topic = {}", record.key(), record.value(), receivedTopic);

        ack.acknowledge();
    }

}
