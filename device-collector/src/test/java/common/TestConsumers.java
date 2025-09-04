package common;

import lombok.extern.slf4j.Slf4j;
import org.ourcode.devicecollector.kafka.configuration.KafkaTopics;

@Slf4j
public class TestConsumers {

    private final KafkaTopics kafkaTopics;
    private final String bootstrapServers;
    private final String schemaRegistryUrl;


    public TestConsumers(KafkaTopics kafkaTopics, String bootstrapServers, String schemaRegistryUrl) {
        this.kafkaTopics = kafkaTopics;
        this.bootstrapServers = bootstrapServers;
        this.schemaRegistryUrl = schemaRegistryUrl;
    }

    void init() {

    }

    void close() {

    }

}
