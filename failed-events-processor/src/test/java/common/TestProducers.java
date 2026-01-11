package common;

import io.confluent.kafka.serializers.KafkaAvroSerializer;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.ourcode.avro.DeviceDeadLetter;
import org.ourcode.devicecollector.kafka.configuration.KafkaTopics;

import java.util.List;
import java.util.Properties;
import java.util.function.Function;

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

    public void sendDeviceDeadLetters(List<DeviceDeadLetter> deadLetters) {
        publish(
                deadLetters,
                DeviceDeadLetter::getDeviceId,
                kafkaTopics.deviceIdsDlt()
        );
    }

    public void sendEventDeadLetters(List<org.ourcode.avro.DeviceEventDeadLetter> deadLetters) {
        publish(
                deadLetters,
                org.ourcode.avro.DeviceEventDeadLetter::getEventId,
                kafkaTopics.eventsDlt()
        );
    }

    private <T> void publish(List<T> objects, Function<T, String> keyExtractor, String topic) {
        try (var eventProducer = getDeviceDltKafkaProducer()) {
            for (T dlt : objects) {
                eventProducer.send(new ProducerRecord<>(topic, keyExtractor.apply(dlt), dlt)).get();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to send events", e);
        }
    }

    public void sendRawEventBytes(String topic, String key, byte[] payload) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());

        try (var producer = new KafkaProducer<String, byte[]>(props)) {
            producer.send(new ProducerRecord<>(topic, key, payload)).get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to send raw event bytes", e);
        }
    }

    private <T> KafkaProducer<String, T> getDeviceDltKafkaProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
        props.put("schema.registry.url", schemaRegistryUrl);

        return new KafkaProducer<>(props);
    }

}
