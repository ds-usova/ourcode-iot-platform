package common;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.ourcode.avro.Device;
import org.ourcode.avro.DeviceEventDeadLetter;
import org.ourcode.eventcollector.kafka.configuration.KafkaTopics;

import java.time.Duration;
import java.util.*;

@Slf4j
public class TestConsumers {

    private final KafkaTopics kafkaTopics;
    private final String bootstrapServers;
    private final String schemaRegistryUrl;

    private KafkaConsumer<String, Device> deviceKafkaConsumer;
    private KafkaConsumer<String, DeviceEventDeadLetter> eventDltKafkaConsumer;

    public TestConsumers(KafkaTopics kafkaTopics, String bootstrapServers, String schemaRegistryUrl) {
        this.kafkaTopics = kafkaTopics;
        this.bootstrapServers = bootstrapServers;
        this.schemaRegistryUrl = schemaRegistryUrl;
    }

    public Map<String, Device> readDevices() {
        return read(deviceKafkaConsumer, kafkaTopics.devices());
    }

    public Map<String, DeviceEventDeadLetter> readDlt() {
        return read(eventDltKafkaConsumer, dltTopic());
    }

    void init() {
        this.deviceKafkaConsumer = createConsumer(kafkaTopics.devices());
        this.eventDltKafkaConsumer = createConsumer(dltTopic());
    }

    void close() {
        if (deviceKafkaConsumer != null) {
            deviceKafkaConsumer.close();
        }

        if (eventDltKafkaConsumer != null) {
            eventDltKafkaConsumer.close();
        }
    }

    private <T> KafkaConsumer<String, T> createConsumer(String topic) {
        KafkaConsumer<String, T> consumer = new KafkaConsumer<>(createProperties());
        consumer.subscribe(Collections.singletonList(topic));

        log.info("Kafka consumer subscribed to topic: {}", consumer.subscription());

        return consumer;
    }

    private Properties createProperties() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, UUID.randomUUID().toString());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put("schema.registry.url", schemaRegistryUrl);
        props.put("specific.avro.reader", "true");

        return props;
    }

    private String dltTopic() {
        return kafkaTopics.events() + "-dlt";
    }

    private <T> Map<String, T> read(KafkaConsumer<String, T> consumer, String topic) {
        Map<String, T> records = new HashMap<>();
        consumer.poll(Duration.ofMillis(500))
                .records(topic)
                .forEach(it -> records.put(it.key(), it.value()));
        return records;
    }

}
