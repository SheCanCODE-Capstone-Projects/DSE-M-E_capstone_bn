-- Run this directly on your production database
-- This will convert all ADMIN roles to DONOR

UPDATE users SET role = 'DONOR' WHERE role = 'ADMIN';
UPDATE role_requests SET requested_role = 'DONOR' WHERE requested_role = 'ADMIN';

-- Verify the fix
SELECT role, COUNT(*) FROM users GROUP BY role;
