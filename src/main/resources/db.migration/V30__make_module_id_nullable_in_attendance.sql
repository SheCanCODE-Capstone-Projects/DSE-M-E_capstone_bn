-- Make module_id nullable in attendance table
-- This allows attendance tracking without requiring specific modules

ALTER TABLE attendance ALTER COLUMN module_id DROP NOT NULL;

-- Drop the foreign key constraint temporarily
ALTER TABLE attendance DROP CONSTRAINT IF EXISTS attendance_modules_fk;

-- Re-add the foreign key constraint
ALTER TABLE attendance
ADD CONSTRAINT attendance_modules_fk
    FOREIGN KEY (module_id)
    REFERENCES training_modules(module_id)
    ON DELETE SET NULL
    ON UPDATE CASCADE;
