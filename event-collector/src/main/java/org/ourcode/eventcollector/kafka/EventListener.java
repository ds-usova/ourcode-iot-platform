package org.ourcode.eventcollector.kafka;

import com.ourcode.avro.DeviceEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class EventListener {

    @KafkaListener(topics = "events", groupId = "${spring.kafka.consumer.group-id}")
    public void listen(ConsumerRecord<String, DeviceEvent> record, Acknowledgment ack) {
        System.out.println("Received event: key " + record.key() + " value " + record.value());
        ack.acknowledge();
    }

}
