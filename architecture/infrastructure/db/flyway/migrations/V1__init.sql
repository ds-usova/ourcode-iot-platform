CREATE TABLE public.devices
(
    device_id   TEXT PRIMARY KEY,
    device_type TEXT   NOT NULL,
    created_at  BIGINT NOT NULL,
    meta        TEXT
);