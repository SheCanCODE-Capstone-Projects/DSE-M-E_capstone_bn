-- Add verification fields to participants table
ALTER TABLE participants
    ADD COLUMN IF NOT EXISTS is_verified BOOLEAN DEFAULT FALSE NOT NULL,
    ADD COLUMN IF NOT EXISTS verified_by UUID,
    ADD COLUMN IF NOT EXISTS verified_at TIMESTAMP;

-- Add foreign key constraint for verified_by
ALTER TABLE participants
    ADD CONSTRAINT participants_verified_by_fk
        FOREIGN KEY (verified_by)
            REFERENCES users(user_id)
            ON DELETE RESTRICT
            ON UPDATE CASCADE;

-- Add index for verification queries
CREATE INDEX IF NOT EXISTS idx_participants_verified ON participants(is_verified);
CREATE INDEX IF NOT EXISTS idx_participants_verified_by ON participants(verified_by);
