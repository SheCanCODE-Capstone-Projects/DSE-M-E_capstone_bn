-- ============================================================================
-- DATABASE CLEANUP MIGRATION SCRIPT
-- Purpose: Remove duplicate cohort tables and simplify schema
-- 
-- IMPORTANT: 
-- 1. BACKUP YOUR DATABASE FIRST!
-- 2. This will run automatically via Flyway
-- 3. Test on a copy of production first
-- ============================================================================

-- ============================================================================
-- STEP 1: DROP FACILITATORS_COHORT_BATCHES (redundant)
-- ============================================================================
DROP TABLE IF EXISTS facilitators_cohort_batches CASCADE;

-- ============================================================================
-- STEP 2: REMOVE program_id FROM me_cohorts (redundant)
-- Can get program from batch → partner
-- ============================================================================
ALTER TABLE me_cohorts DROP COLUMN IF EXISTS program_id CASCADE;

-- ============================================================================
-- STEP 3: MIGRATE ENROLLMENTS FROM cohorts TO me_cohorts
-- ============================================================================

-- Add temporary column if enrollments references old cohorts table
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'enrollments' 
        AND column_name = 'cohort_id'
        AND table_schema = 'public'
    ) THEN
        -- Check if cohort_id references old cohorts table
        IF EXISTS (
            SELECT 1 FROM information_schema.table_constraints tc
            JOIN information_schema.constraint_column_usage ccu 
                ON tc.constraint_name = ccu.constraint_name
            WHERE tc.table_name = 'enrollments'
            AND ccu.table_name = 'cohorts'
            AND tc.constraint_type = 'FOREIGN KEY'
        ) THEN
            -- Add new column for me_cohorts
            ALTER TABLE enrollments ADD COLUMN IF NOT EXISTS me_cohort_id UUID;
            
            -- Migrate data: match by name and dates
            UPDATE enrollments e
            SET me_cohort_id = mc.cohort_id
            FROM cohorts c
            JOIN me_cohorts mc ON LOWER(mc.name) = LOWER(c.cohort_name)
            WHERE e.cohort_id = c.cohort_id
            AND e.me_cohort_id IS NULL;
            
            -- Drop old foreign key
            ALTER TABLE enrollments DROP CONSTRAINT IF EXISTS fk_enrollments_cohort;
            ALTER TABLE enrollments DROP CONSTRAINT IF EXISTS enrollments_cohort_id_fkey;
            
            -- Rename columns
            ALTER TABLE enrollments DROP COLUMN cohort_id;
            ALTER TABLE enrollments RENAME COLUMN me_cohort_id TO cohort_id;
            
            -- Add new foreign key
            ALTER TABLE enrollments
                ADD CONSTRAINT fk_enrollments_me_cohort 
                FOREIGN KEY (cohort_id) 
                REFERENCES me_cohorts(cohort_id) 
                ON DELETE CASCADE;
        END IF;
    END IF;
END $$;

-- ============================================================================
-- STEP 4: DROP OLD COHORTS TABLE
-- ============================================================================
DROP TABLE IF EXISTS cohorts CASCADE;

-- ============================================================================
-- STEP 5: DROP PROGRAMS TABLE (unused, redundant with batches)
-- ============================================================================
DROP TABLE IF EXISTS programs CASCADE;

-- ============================================================================
-- STEP 6: REMOVE facilitator_id FROM me_cohorts (moving to junction table)
-- ============================================================================
ALTER TABLE me_cohorts DROP COLUMN IF EXISTS facilitator_id CASCADE;

-- ============================================================================
-- STEP 7: CREATE me_cohort_facilitators (Multiple facilitators per cohort)
-- ============================================================================
CREATE TABLE IF NOT EXISTS me_cohort_facilitators (
    cohort_id UUID NOT NULL,
    facilitator_id UUID NOT NULL,
    role VARCHAR(50) DEFAULT 'FACILITATOR',
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (cohort_id, facilitator_id),
    CONSTRAINT fk_cohort_facilitators_cohort 
        FOREIGN KEY (cohort_id) 
        REFERENCES me_cohorts(cohort_id) 
        ON DELETE CASCADE,
    CONSTRAINT fk_cohort_facilitators_facilitator 
        FOREIGN KEY (facilitator_id) 
        REFERENCES facilitators(facilitator_id) 
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_cohort_facilitators_cohort ON me_cohort_facilitators(cohort_id);
CREATE INDEX IF NOT EXISTS idx_cohort_facilitators_facilitator ON me_cohort_facilitators(facilitator_id);

-- ============================================================================
-- STEP 8: ENSURE PROPER CONSTRAINTS ON ME_COHORTS
-- ============================================================================

-- ============================================================================
-- STEP 8: ENSURE PROPER CONSTRAINTS ON ME_COHORTS
-- ============================================================================

-- Drop existing constraints if they exist
ALTER TABLE me_cohorts DROP CONSTRAINT IF EXISTS fk_me_cohorts_batch;
ALTER TABLE me_cohorts DROP CONSTRAINT IF EXISTS fk_me_cohorts_course;
ALTER TABLE me_cohorts DROP CONSTRAINT IF EXISTS me_cohorts_batch_id_fkey;
ALTER TABLE me_cohorts DROP CONSTRAINT IF EXISTS me_cohorts_course_id_fkey;

-- Add proper constraints
ALTER TABLE me_cohorts
    ADD CONSTRAINT fk_me_cohorts_batch 
        FOREIGN KEY (batch_id) 
        REFERENCES me_cohort_batches(batch_id) 
        ON DELETE CASCADE;

ALTER TABLE me_cohorts
    ADD CONSTRAINT fk_me_cohorts_course 
        FOREIGN KEY (course_id) 
        REFERENCES courses(course_id) 
        ON DELETE RESTRICT;

-- ============================================================================
-- STEP 9: ENSURE PROPER CONSTRAINTS ON ME_COHORT_BATCHES
-- ============================================================================

-- Drop existing constraints if they exist
ALTER TABLE me_cohort_batches DROP CONSTRAINT IF EXISTS fk_me_cohort_batches_partner;
ALTER TABLE me_cohort_batches DROP CONSTRAINT IF EXISTS fk_me_cohort_batches_center;
ALTER TABLE me_cohort_batches DROP CONSTRAINT IF EXISTS me_cohort_batches_partner_id_fkey;
ALTER TABLE me_cohort_batches DROP CONSTRAINT IF EXISTS me_cohort_batches_center_id_fkey;

-- Add proper constraints
ALTER TABLE me_cohort_batches
    ADD CONSTRAINT fk_me_cohort_batches_partner 
        FOREIGN KEY (partner_id) 
        REFERENCES partners(partner_id) 
        ON DELETE CASCADE;

ALTER TABLE me_cohort_batches
    ADD CONSTRAINT fk_me_cohort_batches_center 
        FOREIGN KEY (center_id) 
        REFERENCES centers(center_id) 
        ON DELETE SET NULL;

-- ============================================================================
-- STEP 10: CREATE INDEXES FOR PERFORMANCE
-- ============================================================================

CREATE INDEX IF NOT EXISTS idx_me_cohorts_batch_id ON me_cohorts(batch_id);
CREATE INDEX IF NOT EXISTS idx_me_cohorts_course_id ON me_cohorts(course_id);
CREATE INDEX IF NOT EXISTS idx_me_cohorts_status ON me_cohorts(status);

CREATE INDEX IF NOT EXISTS idx_me_cohort_batches_partner_id ON me_cohort_batches(partner_id);
CREATE INDEX IF NOT EXISTS idx_me_cohort_batches_center_id ON me_cohort_batches(center_id);
CREATE INDEX IF NOT EXISTS idx_me_cohort_batches_status ON me_cohort_batches(status);

-- ============================================================================
-- MIGRATION COMPLETE
-- ============================================================================
