CREATE TABLE training_modules (
                                  module_id      uuid PRIMARY KEY,
                                  program_id     uuid NOT NULL,
                                  module_name    varchar(255) NOT NULL,
                                  description    text,
                                  sequence_order integer,
                                  duration_hours decimal(5,2),
                                  is_mandatory   boolean DEFAULT false NOT NULL,
                                  created_at     timestamp NOT NULL,
                                  updated_at     timestamp,

                                  CONSTRAINT training_modules_programs_fk
                                      FOREIGN KEY (program_id)
                                          REFERENCES programs(program_id)
                                          ON DELETE CASCADE
                                          ON UPDATE CASCADE
);
