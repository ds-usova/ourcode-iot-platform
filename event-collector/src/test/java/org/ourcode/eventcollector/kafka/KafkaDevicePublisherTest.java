package org.ourcode.eventcollector.kafka;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.ourcode.eventcollector.api.exception.MessageNotPublishedException;
import org.ourcode.eventcollector.api.gateway.DevicePublisherListener;
import org.ourcode.eventcollector.api.model.DeviceEvent;
import org.ourcode.eventcollector.kafka.configuration.KafkaTopics;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class KafkaDevicePublisherTest {

    private KafkaTemplate<String, Object> kafkaTemplate;
    private DevicePublisherListener listener;

    private DeviceEvent validEvent;

    private KafkaDevicePublisher target;

    @BeforeEach
    void setUp() {
        kafkaTemplate = mock(KafkaTemplate.class);
        listener = mock(DevicePublisherListener.class);

        validEvent = mock(DeviceEvent.class);
        when(validEvent.deviceId()).thenReturn("device-123");

        KafkaTopics kafkaTopics = mock(KafkaTopics.class);
        when(kafkaTopics.devices()).thenReturn("device-ids");

        target = new KafkaDevicePublisher(kafkaTemplate, kafkaTopics, List.of(listener));
    }

    @Test
    @DisplayName("publish sends messages successfully")
    void publish_SendsMessages() {
        // Given: a successful send
        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        future.complete(mock(SendResult.class));
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(future);

        // When: event is published
        assertDoesNotThrow(() -> target.publish(validEvent));

        // Then: the message is sent to kafka and no error is reported
        verify(kafkaTemplate, times(1)).send(any(ProducerRecord.class));
        verify(listener, never()).onError(any(), any());
    }

    @Test
    @DisplayName("publish notifies listeners on error")
    void publish_NotifiesListenersOnError() {
        // Given: an error occurred during send
        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        Throwable cause = new RuntimeException("Kafka error");
        future.completeExceptionally(cause);
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(future);

        // When: event is published
        target.publish(validEvent);

        // Then: the listener is notified
        verify(listener, atLeastOnce()).onError(validEvent, cause);
    }

    @Test
    void publish_exception_thrown() {
        RuntimeException ex = new RuntimeException("send failed");
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenThrow(ex);

        MessageNotPublishedException thrown = assertThrows(MessageNotPublishedException.class, () -> target.publish(validEvent));
        assertThat(thrown.getCause()).isEqualTo(ex);
        verify(listener, atLeastOnce()).onError(validEvent, ex);
    }

}

