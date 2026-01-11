-- Add validation fields to scores table
ALTER TABLE scores
    ADD COLUMN IF NOT EXISTS is_validated BOOLEAN DEFAULT FALSE NOT NULL,
    ADD COLUMN IF NOT EXISTS validated_by UUID,
    ADD COLUMN IF NOT EXISTS validated_at TIMESTAMP;

-- Add foreign key constraint for validated_by
ALTER TABLE scores
    ADD CONSTRAINT scores_validated_by_fk
        FOREIGN KEY (validated_by)
            REFERENCES users(user_id)
            ON DELETE RESTRICT
            ON UPDATE CASCADE;

-- Add index for validation queries
CREATE INDEX IF NOT EXISTS idx_scores_validated ON scores(is_validated);
CREATE INDEX IF NOT EXISTS idx_scores_validated_by ON scores(validated_by);
