-- Add created_by column to employment_outcomes table
ALTER TABLE employment_outcomes
ADD COLUMN created_by UUID REFERENCES users(user_id);

-- Update existing records to set created_by to verified_by if verified_by exists
-- Otherwise, leave as NULL (historical data)
UPDATE employment_outcomes
SET created_by = verified_by
WHERE verified_by IS NOT NULL;
