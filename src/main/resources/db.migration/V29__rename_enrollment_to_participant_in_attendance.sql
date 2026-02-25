-- Rename enrollment_id to participant_id in attendance table to match me_participants
-- This aligns with the new participant system

-- Drop existing constraints
ALTER TABLE attendance DROP CONSTRAINT IF EXISTS attendance_enrollments_fk;
ALTER TABLE attendance DROP CONSTRAINT IF EXISTS unique_enrollment_module_session_date;

-- Rename column
ALTER TABLE attendance RENAME COLUMN enrollment_id TO participant_id;

-- Add new foreign key constraint
ALTER TABLE attendance
ADD CONSTRAINT attendance_participants_fk
    FOREIGN KEY (participant_id)
    REFERENCES me_participants(participant_id)
    ON DELETE CASCADE
    ON UPDATE CASCADE;

-- Add unique constraint with new column name
ALTER TABLE attendance
ADD CONSTRAINT unique_participant_module_session_date 
    UNIQUE (participant_id, module_id, session_date);
