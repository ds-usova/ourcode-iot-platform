package org.ourcode.eventcollector.cassandra;

import org.ourcode.eventcollector.api.model.DeviceEvent;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

@Table("device_events_by_device")
public class DeviceEventEntity {

    @PrimaryKeyColumn(name = "event_id", type = PrimaryKeyType.CLUSTERED)
    private final String eventId;

    @PrimaryKeyColumn(name = "device_id", type = PrimaryKeyType.PARTITIONED)
    private final String deviceId;

    /**
     * The timestamp of the event in milliseconds since epoch.
     */
    @Column("timestamp")
    private final Long timestamp;

    @Column("type")
    private final String eventType;

    /**
     * The payload of the event (JSON/base64).
     */
    @Column("payload")
    private final String payload;

    public DeviceEventEntity(String eventId, String deviceId, Long timestamp, String eventType, String payload) {
        this.eventId = eventId;
        this.deviceId = deviceId;
        this.timestamp = timestamp;
        this.eventType = eventType;
        this.payload = payload;
    }

    public String getEventId() {
        return eventId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }

    @Override
    public String toString() {
        return "DeviceEvent{" +
                "eventId='" + eventId + '\'' +
                ", deviceId='" + deviceId + '\'' +
                ", timestamp=" + timestamp +
                ", eventType='" + eventType + '\'' +
                ", payload='" + payload + '\'' +
                '}';
    }

    public static DeviceEventEntity from(DeviceEvent event) {
        return new DeviceEventEntity(
                event.eventId(),
                event.deviceId(),
                event.timestamp(),
                event.eventType(),
                event.payload()
        );
    }

    public DeviceEvent toModel() {
        return new DeviceEvent(
                this.eventId,
                this.deviceId,
                this.timestamp,
                this.eventType,
                this.payload
        );
    }

}
