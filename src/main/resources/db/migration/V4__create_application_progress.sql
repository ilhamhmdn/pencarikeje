CREATE TABLE application_progress (
    id             BIGSERIAL   PRIMARY KEY,
    application_id BIGINT      NOT NULL REFERENCES applications (id) ON DELETE CASCADE,
    status_id      BIGINT      NOT NULL REFERENCES statuses (id),
    event_date     DATE        NOT NULL,
    notes          TEXT,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
