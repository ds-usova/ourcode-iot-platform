package org.ourcode.devicecollector.kafka.consumer.eventdlt;

import org.ourcode.avro.DeviceDeadLetter;
import org.ourcode.avro.DeviceEventDeadLetter;
import org.ourcode.devicecollector.kafka.consumer.ResilientAvroDeserializer;

import java.util.Base64;

public class EventDltAvroSerializer extends ResilientAvroDeserializer<DeviceEventDeadLetter> {

    @Override
    protected DeviceEventDeadLetter map(byte[] data) {
        return new DeviceEventDeadLetter(null, null, null, null, null,
                "Corrupted message - unable to deserialize dlt",
                Base64.getEncoder().encodeToString(data)
        );
    }

}
