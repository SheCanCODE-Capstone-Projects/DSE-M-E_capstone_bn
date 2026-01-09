-- Add unique constraint to prevent duplicate attendance records
-- One attendance per enrollment per module per session date
ALTER TABLE attendance
ADD CONSTRAINT unique_enrollment_module_session_date 
    UNIQUE (enrollment_id, module_id, session_date);

