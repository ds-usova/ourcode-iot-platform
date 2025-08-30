package common;

import com.ourcode.avro.Device;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.ourcode.eventcollector.kafka.configuration.KafkaTopics;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Slf4j
public class TestConsumers {

    private final KafkaTopics kafkaTopics;
    private final String bootstrapServers;
    private final String schemaRegistryUrl;

    private KafkaConsumer<String, Device> deviceKafkaConsumer;

    public TestConsumers(KafkaTopics kafkaTopics, String bootstrapServers, String schemaRegistryUrl) {
        this.kafkaTopics = kafkaTopics;
        this.bootstrapServers = bootstrapServers;
        this.schemaRegistryUrl = schemaRegistryUrl;
    }

    public Map<String, Device> readDevices() {
        Map<String, Device> devices = new HashMap<>();
        deviceKafkaConsumer.poll(Duration.ofMillis(1000))
                .records(kafkaTopics.devices())
                .forEach(it -> devices.put(it.key(), it.value()));
        return devices;
    }

    void init() {
        this.deviceKafkaConsumer = createDeviceConsumer();
    }

    void close() {
        if (deviceKafkaConsumer != null) {
            deviceKafkaConsumer.close();
        }
    }

    private KafkaConsumer<String, Device> createDeviceConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put("schema.registry.url", schemaRegistryUrl);
        props.put("specific.avro.reader", "true");

        KafkaConsumer<String, Device> deviceKafkaConsumer = new KafkaConsumer<>(props);
        deviceKafkaConsumer.subscribe(Collections.singletonList(kafkaTopics.devices()));

        log.info("Kafka consumer subscribed to topic: {}", kafkaTopics.devices());

        return deviceKafkaConsumer;
    }

}
