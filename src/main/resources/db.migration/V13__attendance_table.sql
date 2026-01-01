CREATE TABLE attendance (
                            attendance_id uuid PRIMARY KEY,
                            enrollment_id uuid NOT NULL,
                            module_id     uuid NOT NULL,
                            session_date  date NOT NULL,
                            status        varchar(20) NOT NULL,
                            remarks       text,
                            recorded_by   uuid,
                            created_at    timestamp NOT NULL,
                            updated_at    timestamp,

                            CONSTRAINT check_attendance_status
                                CHECK (status IN ('PRESENT', 'ABSENT', 'LATE', 'EXCUSED')),


                            CONSTRAINT attendance_enrollments_fk
                                FOREIGN KEY (enrollment_id)
                                    REFERENCES enrollments(enrollment_id)
                                    ON DELETE CASCADE
                                    ON UPDATE CASCADE,

                            CONSTRAINT attendance_modules_fk
                                FOREIGN KEY (module_id)
                                    REFERENCES training_modules(module_id)
                                    ON DELETE RESTRICT
                                    ON UPDATE CASCADE,

                            CONSTRAINT attendance_recorded_by_fk
                                FOREIGN KEY (recorded_by)
                                    REFERENCES users(user_id)
                                    ON DELETE SET NULL
                                    ON UPDATE CASCADE
);
