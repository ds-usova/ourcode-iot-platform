CREATE SCHEMA IF NOT EXISTS iot;

CREATE TABLE iot.devices
(
    device_id   TEXT PRIMARY KEY,
    name        TEXT NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    meta        TEXT
);