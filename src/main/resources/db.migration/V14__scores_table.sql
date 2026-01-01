CREATE TABLE scores (
                        score_id        uuid PRIMARY KEY,
                        enrollment_id   uuid NOT NULL,
                        module_id       uuid NOT NULL,

                        assessment_type varchar(20) NOT NULL,
                        score_value     decimal(5,2) NOT NULL
                            CHECK (score_value BETWEEN 0 AND 100),

                        recorded_by     uuid NOT NULL,
                        recorded_at     timestamp NOT NULL,

                        created_at      timestamp NOT NULL,
                        updated_at      timestamp,

                        CONSTRAINT check_assessment_type
                            CHECK (assessment_type IN ('QUIZ', 'ASSIGNMENT', 'EXAM', 'CAPSTONE', 'OTHER')),

                        CONSTRAINT scores_enrollments_fk
                            FOREIGN KEY (enrollment_id)
                                REFERENCES enrollments(enrollment_id)
                                ON DELETE CASCADE
                                ON UPDATE CASCADE,

                        CONSTRAINT scores_modules_fk
                            FOREIGN KEY (module_id)
                                REFERENCES training_modules(module_id)
                                ON DELETE RESTRICT
                                ON UPDATE CASCADE,

                        CONSTRAINT scores_recorded_by_fk
                            FOREIGN KEY (recorded_by)
                                REFERENCES users(user_id)
                                ON DELETE SET NULL
                                ON UPDATE CASCADE
);
