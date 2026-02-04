-- Initial schema for Router Manager
-- Creates routers and commands tables

CREATE TABLE routers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    serial_number TEXT UNIQUE NOT NULL,
    last_seen_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT now()
);

CREATE TABLE commands (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    router_id UUID REFERENCES routers(id),
    command_type TEXT NOT NULL, -- for example, REBOOT, PING
    payload JSONB,
    status TEXT NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'SENT', 'ACKED', 'FAILED')),
    created_at TIMESTAMP DEFAULT now(),
    sent_at TIMESTAMP,
    acked_at TIMESTAMP
);
