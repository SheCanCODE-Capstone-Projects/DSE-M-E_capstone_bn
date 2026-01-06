-- Add created_by column to training_modules table
ALTER TABLE training_modules 
ADD COLUMN IF NOT EXISTS created_by uuid;

-- Add foreign key constraint for created_by in training_modules
ALTER TABLE training_modules
ADD CONSTRAINT fk_training_modules_created_by 
    FOREIGN KEY (created_by) 
    REFERENCES users(user_id) 
    ON DELETE SET NULL 
    ON UPDATE CASCADE;

