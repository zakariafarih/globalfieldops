-- ==========================================================
-- V1: Create audit_events table
-- Immutable append-only audit log for business actions
-- ==========================================================

CREATE TABLE audit_events (
    id              UUID            PRIMARY KEY,
    event_type      VARCHAR(100)    NOT NULL,
    service_name    VARCHAR(100)    NOT NULL,
    entity_type     VARCHAR(100)    NOT NULL,
    entity_id       UUID            NOT NULL,
    actor           VARCHAR(255)    NOT NULL,
    details         TEXT,
    correlation_id  VARCHAR(255),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- Filtering indexes: queries typically filter by event_type, entity, service, or time range
CREATE INDEX idx_audit_events_event_type   ON audit_events (event_type);
CREATE INDEX idx_audit_events_entity_id    ON audit_events (entity_id);
CREATE INDEX idx_audit_events_service_name ON audit_events (service_name);
CREATE INDEX idx_audit_events_created_at   ON audit_events (created_at);
