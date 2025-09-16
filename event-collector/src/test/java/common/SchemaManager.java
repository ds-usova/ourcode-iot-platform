package common;

import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.Schema;
import org.ourcode.avro.Device;
import org.ourcode.avro.DeviceEvent;
import org.ourcode.avro.DeviceEventDeadLetter;

import java.io.IOException;

@Slf4j
public class SchemaManager {

    private final SchemaRegistryClient schemaRegistryClient;

    public SchemaManager(String schemaRegistryUrl) {
        this.schemaRegistryClient = new CachedSchemaRegistryClient(schemaRegistryUrl, 1000);
    }

    public void registerSchemas() {
        registerSchema(DeviceEvent.getClassSchema(), "events");
        registerSchema(Device.getClassSchema(), "devices");
        registerSchema(DeviceEventDeadLetter.getClassSchema(), "events-dlt");
    }

    private void registerSchema(Schema schema, String topic) {
        String subject = topic + "-value";

        try {
            schemaRegistryClient.register(subject, new AvroSchema(schema));

            log.info("Registered schema for topic: {}", topic);
        } catch (IOException | RestClientException e) {
            log.error("Error registering schema for topic: {}", topic, e);
            throw new RuntimeException("Failed to register schema", e);
        }
    }

}
