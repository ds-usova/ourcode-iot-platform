package org.ourcode.failedevents.kafka.consumer.devicedlt;

import org.ourcode.avro.DeviceDeadLetter;
import org.ourcode.failedevents.kafka.consumer.ResilientAvroDeserializer;

import java.util.Base64;

public class DeviceDltAvroSerializer extends ResilientAvroDeserializer<DeviceDeadLetter> {

    @Override
    protected DeviceDeadLetter map(byte[] data) {
        return new DeviceDeadLetter(null, null, null, null,
                "Corrupted message - unable to deserialize dlt",
                Base64.getEncoder().encodeToString(data)
        );
    }

}
