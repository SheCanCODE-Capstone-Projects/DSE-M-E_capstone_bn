-- Emergency fix: Convert any remaining ADMIN roles to DONOR
-- This must run before application startup to prevent enum errors

UPDATE users SET role = 'DONOR' WHERE role = 'ADMIN';
UPDATE role_requests SET requested_role = 'DONOR' WHERE requested_role = 'ADMIN';
