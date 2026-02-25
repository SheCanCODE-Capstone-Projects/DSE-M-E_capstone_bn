-- Update unique constraint to be participant + session_date only
-- This allows one attendance record per participant per day (regardless of module)

ALTER TABLE attendance DROP CONSTRAINT IF EXISTS unique_participant_module_session_date;

ALTER TABLE attendance
ADD CONSTRAINT unique_participant_session_date 
    UNIQUE (participant_id, session_date);
