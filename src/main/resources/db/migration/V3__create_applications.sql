CREATE TABLE applications (
    id              BIGSERIAL    PRIMARY KEY,
    user_id         BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    company_name    VARCHAR(255) NOT NULL,
    role_name       VARCHAR(255) NOT NULL,
    job_description TEXT,
    portal_url      TEXT,
    date_applied    DATE         NOT NULL,
    status_id       BIGINT       NOT NULL REFERENCES statuses (id),
    resume_filename VARCHAR(255),
    resume_path     TEXT,
    notes           TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
