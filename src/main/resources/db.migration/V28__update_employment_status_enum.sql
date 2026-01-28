-- Update employment_status check constraint to include INTERNSHIP and TRAINING
ALTER TABLE employment_outcomes
    DROP CONSTRAINT IF EXISTS check_employment_status;

ALTER TABLE employment_outcomes
    ADD CONSTRAINT check_employment_status
        CHECK (employment_status IN ('EMPLOYED', 'INTERNSHIP', 'TRAINING', 'UNEMPLOYED', 'SELF_EMPLOYED', 'FURTHER_EDUCATION'));

