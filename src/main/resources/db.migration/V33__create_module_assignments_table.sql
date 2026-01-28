-- Create module_assignments table
-- Links facilitators to training modules assigned by ME_OFFICER

CREATE TABLE IF NOT EXISTS module_assignments (
    assignment_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    facilitator_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    module_id UUID NOT NULL REFERENCES training_modules(module_id) ON DELETE CASCADE,
    cohort_id UUID NOT NULL REFERENCES cohorts(cohort_id) ON DELETE CASCADE,
    assigned_by UUID NOT NULL REFERENCES users(user_id) ON DELETE RESTRICT,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_facilitator_module_cohort UNIQUE (facilitator_id, module_id, cohort_id)
);

-- Create indexes for better query performance
CREATE INDEX idx_module_assignments_facilitator ON module_assignments(facilitator_id);
CREATE INDEX idx_module_assignments_module ON module_assignments(module_id);
CREATE INDEX idx_module_assignments_cohort ON module_assignments(cohort_id);
CREATE INDEX idx_module_assignments_assigned_by ON module_assignments(assigned_by);
