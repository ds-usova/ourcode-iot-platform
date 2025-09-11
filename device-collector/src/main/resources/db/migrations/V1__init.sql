/*
 public schema has been used, since sharding sphere doesn't support schemas yet
- https://github.com/apache/shardingsphere/issues/36056
- https://github.com/apache/shardingsphere/pull/35022
*/

CREATE TABLE public.devices
(
    device_id   TEXT PRIMARY KEY,
    device_type TEXT   NOT NULL,
    created_at  BIGINT NOT NULL,
    meta        TEXT
);