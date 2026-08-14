CREATE TABLE statuses (
    id            BIGSERIAL    PRIMARY KEY,
    code          VARCHAR(50)  NOT NULL,
    name          VARCHAR(100) NOT NULL,
    display_order INTEGER      NOT NULL,
    is_active     BOOLEAN      NOT NULL DEFAULT true,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_statuses_code UNIQUE (code)
);
