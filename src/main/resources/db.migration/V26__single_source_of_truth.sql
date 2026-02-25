-- Flyway Migration: Single Source of Truth
-- This will run automatically when you start the application

-- Step 1: Add new columns to me_participants
ALTER TABLE me_participants ADD COLUMN IF NOT EXISTS completion_date DATE;
ALTER TABLE me_participants ADD COLUMN IF NOT EXISTS dropout_date DATE;
ALTER TABLE me_participants ADD COLUMN IF NOT EXISTS dropout_reason TEXT;
ALTER TABLE me_participants ADD COLUMN IF NOT EXISTS is_verified BOOLEAN DEFAULT FALSE;
ALTER TABLE me_participants ADD COLUMN IF NOT EXISTS verified_by UUID;
ALTER TABLE me_participants ADD COLUMN IF NOT EXISTS created_by UUID;

-- Step 2: Add program_id to me_cohorts
ALTER TABLE me_cohorts ADD COLUMN IF NOT EXISTS program_id UUID;

-- Step 3: Rename enrollment_id to participant_id in attendance
ALTER TABLE attendance RENAME COLUMN enrollment_id TO participant_id;

-- Step 4: Rename enrollment_id to participant_id in scores
ALTER TABLE scores RENAME COLUMN enrollment_id TO participant_id;

-- Step 5: Add foreign key constraints (optional)
ALTER TABLE me_participants ADD CONSTRAINT IF NOT EXISTS fk_me_participants_verified_by FOREIGN KEY (verified_by) REFERENCES users(user_id);
ALTER TABLE me_participants ADD CONSTRAINT IF NOT EXISTS fk_me_participants_created_by FOREIGN KEY (created_by) REFERENCES users(user_id);
ALTER TABLE me_cohorts ADD CONSTRAINT IF NOT EXISTS fk_me_cohorts_program FOREIGN KEY (program_id) REFERENCES programs(program_id);
