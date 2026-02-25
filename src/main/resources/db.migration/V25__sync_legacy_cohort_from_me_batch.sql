-- V25: Sync legacy cohort from ME cohort batch
-- This migration creates a legacy cohort from an active ME batch if one doesn't exist
-- Runs automatically on deployment to both local and production databases

-- Only create if no active cohort exists and an active ME batch exists
INSERT INTO cohorts (
    cohort_id,
    cohort_name,
    program_id,
    center_id,
    start_date,
    end_date,
    status,
    target_enrollment,
    created_at,
    updated_at
)
SELECT 
    mb.batch_id,
    mb.name,
    (SELECT program_id FROM programs LIMIT 1),
    mb.center_id,
    mb.start_date,
    mb.end_date,
    mb.status,
    30,
    NOW(),
    NOW()
FROM me_cohort_batches mb
WHERE mb.status = 'ACTIVE'
AND NOT EXISTS (
    SELECT 1 FROM cohorts c WHERE c.status = 'ACTIVE'
)
LIMIT 1;
