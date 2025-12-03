ALTER TABLE users
    ALTER COLUMN role DROP DEFAULT;

ALTER TABLE users
    ALTER COLUMN role TYPE VARCHAR(20)
        USING role::text;

ALTER TABLE users
    ADD CONSTRAINT users_role_check
        CHECK (role IN ('PARTNER', 'ME_OFFICER', 'FACILITATOR', 'UNASSIGNED'));

-- DROP TYPE user_role;