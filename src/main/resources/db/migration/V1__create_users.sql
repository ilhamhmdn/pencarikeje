CREATE TABLE users (
    id            BIGSERIAL    PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_users_email UNIQUE (email)
);

-- Case-insensitive uniqueness enforced by the database rather than trusted to
-- application code (MVP.md 5.1).
CREATE UNIQUE INDEX uq_users_email_lower ON users (lower(email));
