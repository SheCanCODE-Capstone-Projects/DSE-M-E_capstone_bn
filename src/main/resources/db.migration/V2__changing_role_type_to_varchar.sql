ALTER TABLE users
    ALTER COLUMN role DROP DEFAULT;

ALTER TABLE users
    ALTER COLUMN role TYPE VARCHAR(20)
        USING role::text;

-- Drop constraint if it exists, then add it
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;

ALTER TABLE users
    ADD CONSTRAINT users_role_check
        CHECK (role IN ('ADMIN', 'PARTNER', 'ME_OFFICER', 'FACILITATOR', 'UNASSIGNED'));

-- DROP TYPE user_role;