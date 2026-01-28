-- Add monthly_amount column to employment_outcomes table
ALTER TABLE employment_outcomes
    ADD COLUMN monthly_amount decimal(10,2);

-- Add comment
COMMENT ON COLUMN employment_outcomes.monthly_amount IS 'Monthly salary or stipend amount in local currency (e.g., GH₵).';

