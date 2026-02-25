-- Auto-link courses to batches when course is created
-- This ensures when ME Officer creates a course, it's automatically available in all batches

CREATE OR REPLACE FUNCTION auto_link_course_to_batches()
RETURNS TRIGGER AS $$
BEGIN
    -- When a new course is created with ACTIVE status, link it to all existing batches
    IF NEW.status = 'ACTIVE' THEN
        INSERT INTO me_cohorts (
            cohort_id,
            name,
            batch_id,
            course_id,
            start_date,
            end_date,
            max_participants,
            status,
            created_at,
            updated_at
        )
        SELECT 
            gen_random_uuid(),
            mb.name || ' - ' || NEW.name,
            mb.batch_id,
            NEW.course_id,
            mb.start_date,
            mb.end_date,
            30,
            'UPCOMING',
            NOW(),
            NOW()
        FROM me_cohort_batches mb
        WHERE NOT EXISTS (
            SELECT 1 FROM me_cohorts mc 
            WHERE mc.batch_id = mb.batch_id 
            AND mc.course_id = NEW.course_id
        );
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger for new courses
DROP TRIGGER IF EXISTS trigger_auto_link_course ON courses;
CREATE TRIGGER trigger_auto_link_course
AFTER INSERT ON courses
FOR EACH ROW
EXECUTE FUNCTION auto_link_course_to_batches();

-- Also create trigger for when course status changes to ACTIVE
CREATE OR REPLACE FUNCTION auto_link_course_on_activate()
RETURNS TRIGGER AS $$
BEGIN
    -- When course status changes to ACTIVE, link it to all batches
    IF OLD.status != 'ACTIVE' AND NEW.status = 'ACTIVE' THEN
        INSERT INTO me_cohorts (
            cohort_id,
            name,
            batch_id,
            course_id,
            start_date,
            end_date,
            max_participants,
            status,
            created_at,
            updated_at
        )
        SELECT 
            gen_random_uuid(),
            mb.name || ' - ' || NEW.name,
            mb.batch_id,
            NEW.course_id,
            mb.start_date,
            mb.end_date,
            30,
            'UPCOMING',
            NOW(),
            NOW()
        FROM me_cohort_batches mb
        WHERE NOT EXISTS (
            SELECT 1 FROM me_cohorts mc 
            WHERE mc.batch_id = mb.batch_id 
            AND mc.course_id = NEW.course_id
        );
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_auto_link_course_on_activate ON courses;
CREATE TRIGGER trigger_auto_link_course_on_activate
AFTER UPDATE ON courses
FOR EACH ROW
EXECUTE FUNCTION auto_link_course_on_activate();
