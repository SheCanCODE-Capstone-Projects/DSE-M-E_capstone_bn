-- Add assessment_name column to scores table
ALTER TABLE scores
    ADD COLUMN assessment_name VARCHAR(255);

-- Add comment
COMMENT ON COLUMN scores.assessment_name IS 'Name of the assessment (e.g., "Midterm Exam", "Project 1", "Quiz 3")';

