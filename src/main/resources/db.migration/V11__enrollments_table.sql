CREATE TABLE enrollments (
                             enrollment_id   uuid PRIMARY KEY,
                             participant_id  uuid NOT NULL,
                             cohort_id       uuid NOT NULL,
                             enrollment_date date NOT NULL,
                             status          varchar(20) NOT NULL,
                             completion_date date,
                             dropout_date    date,
                             dropout_reason  text,
                             is_verified     boolean NOT NULL,
                             verified_by     uuid,
                             created_at      timestamp NOT NULL,
                             updated_at      timestamp,

                             CONSTRAINT check_enrollment_status
                                 CHECK (status IN ('ENROLLED', 'ACTIVE', 'COMPLETED', 'DROPPED_OUT', 'WITHDRAWN')),


                             CONSTRAINT enrollments_participants_fk
                                 FOREIGN KEY (participant_id)
                                     REFERENCES participants(participant_id)
                                     ON DELETE CASCADE
                                     ON UPDATE CASCADE,

                             CONSTRAINT enrollments_cohorts_fk
                                 FOREIGN KEY (cohort_id)
                                     REFERENCES cohorts(cohort_id)
                                     ON DELETE RESTRICT
                                     ON UPDATE CASCADE
);
