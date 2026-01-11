package org.ourcode.devicecollector.kafka.consumer.eventdlt;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.ourcode.avro.DeviceEventDeadLetter;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class DeviceEventDeadLetterKafkaConsumer {

    @KafkaListener(
            topics = "${app.kafka.topics.events-dlt}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "deviceEventDeadLetterKafkaListenerContainerFactory"
    )
    public void listen(
            List<ConsumerRecord<String, DeviceEventDeadLetter>> records,
            Acknowledgment ack
    ) {
        log.debug("Received {} device event dead letter records", records.size());

        for (ConsumerRecord<String, DeviceEventDeadLetter> record : records) {
            DeviceEventDeadLetter deadLetter = record.value();

            log.error("Processing Device Event DLT - EventId: {}, DeviceId: {}, Type: {}, Error: {}",
                    deadLetter.getEventId(),
                    deadLetter.getDeviceId(),
                    deadLetter.getType(),
                    deadLetter.getErrorMessage());
        }

        ack.acknowledge();
        log.debug("Successfully processed {} device event dead letter records", records.size());
    }

}

