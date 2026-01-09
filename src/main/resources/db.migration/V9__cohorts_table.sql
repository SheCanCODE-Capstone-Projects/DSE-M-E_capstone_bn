CREATE TABLE cohorts (
                         cohort_id         uuid PRIMARY KEY,
                         program_id        uuid NOT NULL,
                         center_id         uuid NOT NULL,
                         cohort_name       varchar(255) UNIQUE NOT NULL,
                         start_date        date NOT NULL,
                         end_date          date NOT NULL,
                         status            varchar(20) NOT NULL,
                         target_enrollment integer NOT NULL,
                         created_at        timestamp NOT NULL,
                         updated_at        timestamp,

                         CONSTRAINT check_cohort_status
                             CHECK (status IN ('PLANNED', 'ACTIVE', 'COMPLETED', 'CANCELLED')),

                         CONSTRAINT cohorts_programs_fk
                             FOREIGN KEY (program_id)
                                 REFERENCES programs(program_id)
                                 ON DELETE CASCADE
                                 ON UPDATE CASCADE,

                         CONSTRAINT cohorts_centers_fk
                             FOREIGN KEY (center_id)
                                 REFERENCES centers(center_id)
                                 ON DELETE CASCADE
                                 ON UPDATE CASCADE
);
