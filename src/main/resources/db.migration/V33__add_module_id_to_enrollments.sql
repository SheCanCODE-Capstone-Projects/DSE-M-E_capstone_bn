-- Add module_id to enrollments table
-- When facilitator enrolls a participant, they're tied to a specific module

ALTER TABLE enrollments
ADD COLUMN IF NOT EXISTS module_id UUID REFERENCES training_modules(module_id) ON DELETE SET NULL;

-- Create index for better query performance
CREATE INDEX IF NOT EXISTS idx_enrollments_module ON enrollments(module_id);

-- Add comment
COMMENT ON COLUMN enrollments.module_id IS 'Training module this enrollment is associated with. Set when facilitator enrolls participant to their assigned module.';
