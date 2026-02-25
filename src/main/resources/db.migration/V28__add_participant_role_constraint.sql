-- Add PARTICIPANT role to users table constraint
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;

ALTER TABLE users ADD CONSTRAINT users_role_check 
CHECK (role IN ('FACILITATOR', 'ME_OFFICER', 'DONOR', 'UNASSIGNED', 'PARTICIPANT'));
