// Create keyspace
CREATE KEYSPACE IF NOT EXISTS iot_schema
WITH replication = {'class': 'SimpleStrategy', 'replication_factor':1};

// Create table under keyspace
CREATE TABLE IF NOT EXISTS iot_schema.device_events_by_device
(
    device_id text,
    event_id  text,
    timestamp bigint,
    type      text,
    payload   text,
    PRIMARY KEY (device_id, event_id)
);