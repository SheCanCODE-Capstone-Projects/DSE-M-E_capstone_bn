-- Manual Role Enum Migration Script
-- Run this script manually if the automatic migration fails
-- This script is safe to run multiple times

-- Check current role distribution before migration
SELECT 'BEFORE MIGRATION - Users by role:' as info;
SELECT role, COUNT(*) as count FROM users GROUP BY role;

SELECT 'BEFORE MIGRATION - Role requests by requested_role:' as info;
SELECT requested_role, COUNT(*) as count FROM role_requests GROUP BY requested_role;

-- Step 1: Backfill existing PARTNER records to DONOR in users table
UPDATE users 
SET role = 'DONOR' 
WHERE role = 'PARTNER';

-- Step 2: Backfill existing PARTNER records to DONOR in role_requests table  
UPDATE role_requests 
SET requested_role = 'DONOR' 
WHERE requested_role = 'PARTNER';

-- Step 3: Update CHECK constraint in users table to match new Role enum
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;

ALTER TABLE users
    ADD CONSTRAINT users_role_check
        CHECK (role IN ('ADMIN', 'FACILITATOR', 'ME_OFFICER', 'DONOR', 'UNASSIGNED'));

-- Step 4: Update CHECK constraint in role_requests table to match new Role enum
ALTER TABLE role_requests DROP CONSTRAINT IF EXISTS check_requested_role;

ALTER TABLE role_requests
    ADD CONSTRAINT check_requested_role
        CHECK (requested_role IN ('FACILITATOR', 'ME_OFFICER', 'DONOR'));

-- Verify migration results
SELECT 'AFTER MIGRATION - Users by role:' as info;
SELECT role, COUNT(*) as count FROM users GROUP BY role;

SELECT 'AFTER MIGRATION - Role requests by requested_role:' as info;
SELECT requested_role, COUNT(*) as count FROM role_requests GROUP BY requested_role;

-- Test constraint by trying to insert invalid role (should fail)
-- INSERT INTO users (user_id, email, password_hash, role) VALUES (gen_random_uuid(), 'test@test.com', 'hash', 'INVALID_ROLE');

SELECT 'Migration completed successfully!' as status;