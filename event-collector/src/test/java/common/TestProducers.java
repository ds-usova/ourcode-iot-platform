package common;

import com.ourcode.avro.DeviceEvent;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.ourcode.eventcollector.kafka.configuration.KafkaTopics;

import java.util.List;
import java.util.Properties;

@Slf4j
public class TestProducers {

    private final KafkaTopics kafkaTopics;
    private final String bootstrapServers;
    private final String schemaRegistryUrl;

    public TestProducers(KafkaTopics kafkaTopics, String bootstrapServers, String schemaRegistryUrl) {
        this.kafkaTopics = kafkaTopics;
        this.bootstrapServers = bootstrapServers;
        this.schemaRegistryUrl = schemaRegistryUrl;
    }

    public void sendEvents(List<DeviceEvent> deviceEvents) {
        try (var eventProducer = getDeviceEventKafkaProducer()) {
            for (DeviceEvent event : deviceEvents) {
                eventProducer.send(new ProducerRecord<>(kafkaTopics.events(), event.getEventId(), event)).get();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to send events", e);
        }
    }

    public void sendRawEventBytes(String key, byte[] payload) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());

        try (var producer = new KafkaProducer<String, byte[]>(props)) {
            producer.send(new ProducerRecord<>(kafkaTopics.events(), key, payload)).get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to send raw event bytes", e);
        }
    }

    private KafkaProducer<String, DeviceEvent> getDeviceEventKafkaProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
        props.put("schema.registry.url", schemaRegistryUrl);

        return new KafkaProducer<>(props);
    }

}
