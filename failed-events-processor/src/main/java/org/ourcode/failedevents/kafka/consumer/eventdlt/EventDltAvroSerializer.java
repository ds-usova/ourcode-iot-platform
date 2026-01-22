package org.ourcode.failedevents.kafka.consumer.eventdlt;

import org.ourcode.avro.DeviceEventDeadLetter;
import org.ourcode.failedevents.kafka.configuration.ResilientAvroDeserializer;

import java.util.Base64;

public class EventDltAvroSerializer extends ResilientAvroDeserializer<DeviceEventDeadLetter> {

    @Override
    protected DeviceEventDeadLetter map(byte[] data) {
        return new DeviceEventDeadLetter(null, null, null, null, null,
                "DeserializationException",
                "Corrupted message - unable to deserialize dlt",
                Base64.getEncoder().encodeToString(data)
        );
    }

}
