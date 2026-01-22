package org.ourcode.failedevents.kafka.configuration;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.ourcode.avro.DeviceDeadLetter;
import org.ourcode.avro.DeviceEventDeadLetter;
import org.ourcode.failedevents.kafka.consumer.devicedlt.DeviceDltAvroSerializer;
import org.ourcode.failedevents.kafka.consumer.eventdlt.EventDltAvroSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfiguration {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Value("${spring.kafka.consumer.auto-offset-reset}")
    private String autoOffsetReset;

    @Value("${spring.kafka.consumer.properties.schema.registry.url}")
    private String schemaRegistryUrl;

    @Value("${spring.kafka.consumer.properties.specific.avro.reader}")
    private boolean specificAvroReader;

    @Value("${spring.kafka.consumer.concurrency}")
    private int listenerConcurrency;

    @Value("${spring.kafka.consumer.properties.max.poll.records}")
    private int maxPollRecords;

    @Value("${spring.kafka.consumer.backoff-interval}")
    private long backoffInterval;

    // ====================================================================================================
    // DeviceDeadLetter Topic Configuration
    // ====================================================================================================

    @Bean
    public ConsumerFactory<String, DeviceDeadLetter> deviceDeadLetterConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);

        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, DeviceDltAvroSerializer.class.getName());

        // schema registry
        props.put("schema.registry.url", schemaRegistryUrl);
        props.put("specific.avro.reader", specificAvroReader);

        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPollRecords);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, DeviceDeadLetter> deviceDeadLetterKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, DeviceDeadLetter> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(deviceDeadLetterConsumerFactory());
        factory.setConcurrency(listenerConcurrency);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setBatchListener(true);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(new FixedBackOff(backoffInterval, Long.MAX_VALUE));
        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }

    // ====================================================================================================
    // DeviceEventDeadLetter Topic Configuration
    // ====================================================================================================

    @Bean
    public ConsumerFactory<String, DeviceEventDeadLetter> deviceEventDeadLetterConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);

        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, EventDltAvroSerializer.class.getName());

        // schema registry
        props.put("schema.registry.url", schemaRegistryUrl);
        props.put("specific.avro.reader", specificAvroReader);

        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPollRecords);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, DeviceEventDeadLetter> deviceEventDeadLetterKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, DeviceEventDeadLetter> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(deviceEventDeadLetterConsumerFactory());
        factory.setConcurrency(listenerConcurrency);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setBatchListener(true);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(new FixedBackOff(backoffInterval, Long.MAX_VALUE));
        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }

}
