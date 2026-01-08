CREATE TABLE employment_outcomes (
    employment_outcome_id uuid PRIMARY KEY,
    enrollment_id         uuid NOT NULL,
    internship_id         uuid,
    employment_status     varchar(30) NOT NULL,
    employer_name         varchar(255),
    job_title             varchar(255),
    employment_type       varchar(50),
    salary_range          varchar(50),
    start_date            date,
    verified              boolean DEFAULT false NOT NULL,
    verified_by           uuid,
    verified_at           timestamp,
    created_at            timestamp NOT NULL,
    updated_at            timestamp,

    CONSTRAINT check_employment_status
        CHECK (employment_status IN ('EMPLOYED', 'UNEMPLOYED', 'SELF_EMPLOYED', 'FURTHER_EDUCATION')),

    CONSTRAINT check_employment_type
        CHECK (employment_type IN ('FULL_TIME', 'PART_TIME', 'CONTRACT', 'FREELANCE', 'INTERNSHIP')),

    CONSTRAINT employment_enrollments_fk
        FOREIGN KEY (enrollment_id)
            REFERENCES enrollments(enrollment_id)
            ON DELETE CASCADE
            ON UPDATE CASCADE,

    CONSTRAINT employment_internships_fk
        FOREIGN KEY (internship_id)
            REFERENCES internships(internship_id)
            ON DELETE SET NULL
            ON UPDATE CASCADE,

    CONSTRAINT employment_verified_by_fk
        FOREIGN KEY (verified_by)
            REFERENCES users(user_id)
            ON DELETE RESTRICT
            ON UPDATE CASCADE
);

-- Add indexes
CREATE INDEX idx_employment_outcomes_enrollment_id ON employment_outcomes(enrollment_id);
CREATE INDEX idx_employment_outcomes_status ON employment_outcomes(employment_status);
CREATE INDEX idx_employment_outcomes_verified ON employment_outcomes(verified);

