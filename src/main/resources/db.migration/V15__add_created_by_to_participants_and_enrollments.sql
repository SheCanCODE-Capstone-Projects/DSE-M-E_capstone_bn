-- Add created_by column to participants table
ALTER TABLE participants 
ADD COLUMN IF NOT EXISTS created_by uuid;

-- Add foreign key constraint for created_by in participants
ALTER TABLE participants
ADD CONSTRAINT fk_participants_created_by 
    FOREIGN KEY (created_by) 
    REFERENCES users(user_id) 
    ON DELETE SET NULL 
    ON UPDATE CASCADE;

-- Add created_by column to enrollments table
ALTER TABLE enrollments 
ADD COLUMN IF NOT EXISTS created_by uuid;

-- Add foreign key constraint for created_by in enrollments
ALTER TABLE enrollments
ADD CONSTRAINT fk_enrollments_created_by 
    FOREIGN KEY (created_by) 
    REFERENCES users(user_id) 
    ON DELETE SET NULL 
    ON UPDATE CASCADE;

