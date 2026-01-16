package org.ourcode.failedevents.kafka.consumer.devicedlt;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ourcode.avro.DeviceDeadLetter;
import org.ourcode.failedevents.api.service.FailedEventService;
import org.ourcode.failedevents.kafka.configuration.KafkaTopics;
import org.springframework.kafka.support.Acknowledgment;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceDeadLetterKafkaConsumerTest {

    @Mock
    private KafkaTopics kafkaTopics;

    @Mock
    private FailedEventService failedEventService;

    @Mock
    private Acknowledgment acknowledgment;

    private DeviceDeadLetterKafkaConsumer target;

    @BeforeEach
    void setUp() {
        when(kafkaTopics.deviceIdsDlt()).thenReturn("device-ids-dlt");
        target = new DeviceDeadLetterKafkaConsumer(
                kafkaTopics, failedEventService, new ObjectMapper()
        );
    }

    @Test
    @DisplayName("unhappy path - when at least one dead letter can't be saved - then acknowledge is not called")
    void whenOneSaveEventFails_thenAcknowledgeIsNotCalled() {
        // Given: 1. two records are created
        DeviceDeadLetter deadLetter1 = DeviceDeadLetter.newBuilder()
                .setDeviceId("device-001")
                .setDeviceType("sensor")
                .setCreatedAt(Instant.now().toEpochMilli())
                .setException("ValidationException")
                .setErrorMessage("Invalid data")
                .setMeta("{\"key\":\"value\"}")
                .build();

        DeviceDeadLetter deadLetter2 = DeviceDeadLetter.newBuilder()
                .setDeviceId("device-002")
                .setDeviceType("sensor")
                .setCreatedAt(Instant.now().toEpochMilli())
                .setException("NullPointerException")
                .setErrorMessage("Null value encountered")
                .setMeta("{\"key\":\"value\"}")
                .build();

        ConsumerRecord<String, DeviceDeadLetter> record1 = new ConsumerRecord<>(
                "device-ids-dlt", 0, 0L, "device-001", deadLetter1
        );

        ConsumerRecord<String, DeviceDeadLetter> record2 = new ConsumerRecord<>(
                "device-ids-dlt", 0, 1L, "device-002", deadLetter2
        );

        List<ConsumerRecord<String, DeviceDeadLetter>> records = List.of(record1, record2);

        // Given: 2. failedEventService.save succeeds for record1 but fails for record2
        doNothing().when(failedEventService).save(any());
        doThrow(new RuntimeException("Failed to save event"))
                .when(failedEventService)
                .save(argThat(event -> event.id().equals("device-002")));

        // When: processing the batch
        // Then: RuntimeException is thrown and acknowledge is NOT called
        assertThatThrownBy(() -> target.listen(records, acknowledgment))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to process one or more device dead letter records");

        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    @DisplayName("happy path - when all dead letters in the batch successfully saved - then acknowledge is called")
    void whenAllFuturesSucceed_thenAcknowledgeIsCalled() {
        // Given: 1. two records are created
        DeviceDeadLetter deadLetter1 = DeviceDeadLetter.newBuilder()
                .setDeviceId("device-001")
                .setDeviceType("sensor")
                .setCreatedAt(Instant.now().toEpochMilli())
                .setException("ValidationException")
                .setErrorMessage("Invalid data")
                .setMeta("{\"key\":\"value\"}")
                .build();

        DeviceDeadLetter deadLetter2 = DeviceDeadLetter.newBuilder()
                .setDeviceId("device-002")
                .setDeviceType("sensor")
                .setCreatedAt(Instant.now().toEpochMilli())
                .setException("TimeoutException")
                .setErrorMessage("Connection timeout")
                .setMeta("{\"key\":\"value\"}")
                .build();

        ConsumerRecord<String, DeviceDeadLetter> record1 = new ConsumerRecord<>(
                "device-ids-dlt", 0, 0L, "device-001", deadLetter1
        );

        ConsumerRecord<String, DeviceDeadLetter> record2 = new ConsumerRecord<>(
                "device-ids-dlt", 0, 1L, "device-002", deadLetter2
        );

        List<ConsumerRecord<String, DeviceDeadLetter>> records = List.of(record1, record2);

        // Given: 2. failedEventService.save succeeds for both records
        doNothing().when(failedEventService).save(any());

        // When: processing the batch
        target.listen(records, acknowledgment);

        // Then: all records are saved and acknowledge is called once
        verify(failedEventService, times(2)).save(any());
        verify(acknowledgment, times(1)).acknowledge();
    }

}
