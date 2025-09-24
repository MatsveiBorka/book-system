CREATE TABLE IF NOT EXISTS book (
    id UUID PRIMARY KEY NOT NULL,
    title VARCHAR(255),
    author VARCHAR(255),
    publication_year INTEGER,
    description VARCHAR(500),
    created_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ
);
