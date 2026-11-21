CREATE TABLE engagement_notes (
    id BIGSERIAL PRIMARY KEY,
    client_code VARCHAR(64) NOT NULL,
    author VARCHAR(100) NOT NULL,
    status VARCHAR(32) NOT NULL,
    summary TEXT NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_engagement_notes_client_code ON engagement_notes(client_code);
CREATE INDEX idx_engagement_notes_updated_at ON engagement_notes(updated_at DESC);
