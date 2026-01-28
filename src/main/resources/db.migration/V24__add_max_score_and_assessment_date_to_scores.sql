-- Add max_score and assessment_date columns to scores table
ALTER TABLE scores
    ADD COLUMN max_score decimal(5,2) DEFAULT 100.0 NOT NULL;

ALTER TABLE scores
    ADD COLUMN assessment_date date;

-- Add comment
COMMENT ON COLUMN scores.max_score IS 'Maximum possible score for this assessment. Defaults to 100.';
COMMENT ON COLUMN scores.assessment_date IS 'Date when the assessment was conducted. Prioritized over created_at for display purposes.';

