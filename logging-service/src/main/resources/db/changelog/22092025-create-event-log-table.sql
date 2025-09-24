CREATE TABLE IF NOT EXISTS event_log (
    id UUID PRIMARY KEY NOT NULL,
    timestamp TIMESTAMPTZ,
    subject_type VARCHAR(255),
    event_type VARCHAR(255),
    description VARCHAR(1000)
);
