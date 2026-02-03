-- Initial schema for Router Manager
-- Creates routers and commands tables

CREATE TABLE routers (
    id UUID PRIMARY KEY,
    serial_number TEXT UNIQUE NOT NULL,
    last_seen_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT now()
);

CREATE TABLE commands (
    id UUID PRIMARY KEY,
    router_id UUID REFERENCES routers(id),
    command_type TEXT NOT NULL, -- for example, REBOOT, PING
    payload JSONB,
    status TEXT NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'SENT', 'ACKED', 'FAILED')),
    sent_at TIMESTAMP,
    acked_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT now()
);
