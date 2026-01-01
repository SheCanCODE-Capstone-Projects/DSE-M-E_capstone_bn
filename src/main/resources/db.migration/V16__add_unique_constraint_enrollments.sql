-- Add unique constraint to prevent duplicate enrollments
-- A participant cannot be enrolled in the same cohort twice
ALTER TABLE enrollments
ADD CONSTRAINT unique_participant_cohort 
    UNIQUE (participant_id, cohort_id);

