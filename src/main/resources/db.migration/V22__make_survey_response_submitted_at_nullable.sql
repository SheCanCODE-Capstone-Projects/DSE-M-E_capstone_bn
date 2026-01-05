-- Make submitted_at nullable in survey_responses table
-- This allows tracking of pending responses (created but not yet submitted)
ALTER TABLE survey_responses
    ALTER COLUMN submitted_at DROP NOT NULL;

-- Add comment
COMMENT ON COLUMN survey_responses.submitted_at IS 'Date when survey response was submitted. NULL if response is pending (not yet submitted).';

