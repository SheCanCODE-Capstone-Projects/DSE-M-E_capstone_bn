-- Add start_date and end_date columns to surveys table
ALTER TABLE surveys
    ADD COLUMN start_date DATE,
    ADD COLUMN end_date DATE;

-- Add comments
COMMENT ON COLUMN surveys.start_date IS 'Survey start date (when survey becomes available)';
COMMENT ON COLUMN surveys.end_date IS 'Survey end/due date (when survey closes)';

