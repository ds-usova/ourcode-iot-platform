package org.ourcode.failedevents.kafka.consumer.devicedlt;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.ourcode.avro.DeviceDeadLetter;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class DeviceDeadLetterKafkaConsumer {

    @KafkaListener(
            topics = "${app.kafka.topics.device-ids-dlt}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "deviceDeadLetterKafkaListenerContainerFactory"
    )
    public void listen(
            List<ConsumerRecord<String, DeviceDeadLetter>> records,
            Acknowledgment ack
    ) {
        log.debug("Received {} device dead letter records", records.size());

        for (ConsumerRecord<String, DeviceDeadLetter> record : records) {
            DeviceDeadLetter deadLetter = record.value();

            log.error("Processing Device DLT - DeviceId: {}, DeviceType: {}, Error: {}",
                    deadLetter.getDeviceId(),
                    deadLetter.getDeviceType(),
                    deadLetter.getErrorMessage());
        }

        ack.acknowledge();
        log.debug("Successfully processed {} device dead letter records", records.size());
    }
}

