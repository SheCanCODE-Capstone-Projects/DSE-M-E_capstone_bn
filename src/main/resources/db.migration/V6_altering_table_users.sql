ALTER TABLE users
    ADD COLUMN provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL';

ALTER TABLE users
    ADD CONSTRAINT users_provider_check
        CHECK (provider IN ('LOCAL', 'GOOGLE'));

ALTER TABLE users
    ALTER COLUMN password_hash DROP NOT NULL;

