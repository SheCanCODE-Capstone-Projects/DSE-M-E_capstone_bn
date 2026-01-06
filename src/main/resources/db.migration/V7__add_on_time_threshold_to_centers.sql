-- Add on_time_threshold column to centers table
-- Default: 09:00:00 (9 AM) in CAT timezone (GMT+2)
-- This is the time threshold for determining if attendance is PRESENT or LATE
ALTER TABLE centers
    ADD COLUMN on_time_threshold TIME DEFAULT '09:00:00';

-- Add comment
COMMENT ON COLUMN centers.on_time_threshold IS 'Time threshold for attendance. Before this time = PRESENT, at/after = LATE. Default: 9 AM CAT (GMT+2)';

