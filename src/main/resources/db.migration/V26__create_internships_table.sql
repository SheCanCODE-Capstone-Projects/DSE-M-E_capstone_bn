CREATE TABLE internships (
    internship_id   uuid PRIMARY KEY,
    enrollment_id   uuid NOT NULL,
    organization    varchar(255) NOT NULL,
    role_title      varchar(255),
    start_date      date NOT NULL,
    end_date        date,
    status          varchar(30) NOT NULL,
    stipend_amount  decimal(10,2),
    created_by      uuid NOT NULL,
    created_at      timestamp NOT NULL,
    updated_at      timestamp,

    CONSTRAINT check_internship_status
        CHECK (status IN ('PENDING', 'ACTIVE', 'COMPLETED', 'TERMINATED')),

    CONSTRAINT internships_enrollments_fk
        FOREIGN KEY (enrollment_id)
            REFERENCES enrollments(enrollment_id)
            ON DELETE CASCADE
            ON UPDATE CASCADE,

    CONSTRAINT internships_created_by_fk
        FOREIGN KEY (created_by)
            REFERENCES users(user_id)
            ON DELETE RESTRICT
            ON UPDATE CASCADE
);

-- Add indexes
CREATE INDEX idx_internships_enrollment_id ON internships(enrollment_id);
CREATE INDEX idx_internships_status ON internships(status);

