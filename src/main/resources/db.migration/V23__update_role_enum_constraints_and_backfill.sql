-- V23__update_role_enum_constraints_and_backfill.sql
-- Migration to update Role enum constraints and backfill legacy PARTNER values

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