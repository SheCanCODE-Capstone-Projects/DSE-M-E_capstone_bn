-- Replace legacy ADMIN role values now that Role.ADMIN is removed
-- We treat ADMIN as DONOR (top-level owner) in the new model.

-- Update users table: convert ADMIN to DONOR
UPDATE users
SET role = 'DONOR'
WHERE role = 'ADMIN';

-- If any old role_requests rows still refer to ADMIN, map them to DONOR as well
DO $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_name = 'role_requests'
      AND column_name = 'requested_role'
  ) THEN
    UPDATE role_requests
    SET requested_role = 'DONOR'
    WHERE requested_role = 'ADMIN';
  END IF;
END $$;

