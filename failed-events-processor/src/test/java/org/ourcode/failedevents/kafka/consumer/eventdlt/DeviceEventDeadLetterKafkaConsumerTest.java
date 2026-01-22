package org.ourcode.failedevents.kafka.consumer.eventdlt;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ourcode.avro.DeviceEventDeadLetter;
import org.ourcode.failedevents.api.service.FailedEventService;
import org.ourcode.failedevents.kafka.configuration.KafkaTopics;
import org.springframework.kafka.support.Acknowledgment;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceEventDeadLetterKafkaConsumerTest {

    @Mock
    private KafkaTopics kafkaTopics;

    @Mock
    private FailedEventService failedEventService;

    @Mock
    private Acknowledgment acknowledgment;

    private DeviceEventDeadLetterKafkaConsumer target;

    @BeforeEach
    void setUp() {
        when(kafkaTopics.eventsDlt()).thenReturn("events-dlt");
        target = new DeviceEventDeadLetterKafkaConsumer(
                kafkaTopics, failedEventService, new ObjectMapper()
        );
    }

    @Test
    @DisplayName("unhappy path - when at least one dead letter can't be saved - then acknowledge is not called")
    void whenOneSaveEventFails_thenAcknowledgeIsNotCalled() {
        // Given: 1. two records are created
        DeviceEventDeadLetter deadLetter1 = DeviceEventDeadLetter.newBuilder()
                .setEventId("event-001")
                .setDeviceId("device-001")
                .setTimestamp(Instant.now().toEpochMilli())
                .setType("temperature")
                .setPayload("{\"value\":25.5}")
                .setException("ValidationException")
                .setErrorMessage("Invalid data")
                .build();

        DeviceEventDeadLetter deadLetter2 = DeviceEventDeadLetter.newBuilder()
                .setEventId("event-002")
                .setDeviceId("device-002")
                .setTimestamp(Instant.now().toEpochMilli())
                .setType("humidity")
                .setPayload("{\"value\":60}")
                .setException("NullPointerException")
                .setErrorMessage("Null value encountered")
                .build();

        ConsumerRecord<String, DeviceEventDeadLetter> record1 = new ConsumerRecord<>(
                "events-dlt", 0, 0L, "event-001", deadLetter1
        );

        ConsumerRecord<String, DeviceEventDeadLetter> record2 = new ConsumerRecord<>(
                "events-dlt", 0, 1L, "event-002", deadLetter2
        );

        List<ConsumerRecord<String, DeviceEventDeadLetter>> records = List.of(record1, record2);

        // Given: 2. failedEventService.save succeeds for record1 but fails for record2
        doNothing().when(failedEventService).save(any());
        doThrow(new RuntimeException("Failed to save event"))
                .when(failedEventService)
                .save(argThat(event -> event.payload().contains("event-002")));

        // When: processing the batch
        // Then: RuntimeException is thrown and acknowledge is NOT called
        assertThatThrownBy(() -> target.listen(records, acknowledgment))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to process one or more device event dead letter records");

        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    @DisplayName("happy path - when all dead letters in the batch successfully saved - then acknowledge is called")
    void whenAllFuturesSucceed_thenAcknowledgeIsCalled() {
        // Given: 1. two records are created
        DeviceEventDeadLetter deadLetter1 = DeviceEventDeadLetter.newBuilder()
                .setEventId("event-001")
                .setDeviceId("device-001")
                .setTimestamp(Instant.now().toEpochMilli())
                .setType("temperature")
                .setPayload("{\"value\":25.5}")
                .setException("ValidationException")
                .setErrorMessage("Invalid data")
                .build();

        DeviceEventDeadLetter deadLetter2 = DeviceEventDeadLetter.newBuilder()
                .setEventId("event-002")
                .setDeviceId("device-002")
                .setTimestamp(Instant.now().toEpochMilli())
                .setType("humidity")
                .setPayload("{\"value\":60}")
                .setException("TimeoutException")
                .setErrorMessage("Connection timeout")
                .build();

        ConsumerRecord<String, DeviceEventDeadLetter> record1 = new ConsumerRecord<>(
                "events-dlt", 0, 0L, "event-001", deadLetter1
        );

        ConsumerRecord<String, DeviceEventDeadLetter> record2 = new ConsumerRecord<>(
                "events-dlt", 0, 1L, "event-002", deadLetter2
        );

        List<ConsumerRecord<String, DeviceEventDeadLetter>> records = List.of(record1, record2);

        // Given: 2. failedEventService.save succeeds for both records
        doNothing().when(failedEventService).save(any());

        // When: processing the batch
        target.listen(records, acknowledgment);

        // Then: all records are saved and acknowledge is called once
        verify(failedEventService, times(2)).save(any());
        verify(acknowledgment, times(1)).acknowledge();
    }

}
