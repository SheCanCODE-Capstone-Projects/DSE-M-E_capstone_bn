CREATE TABLE programs (
                          program_id     uuid PRIMARY KEY,
                          partner_id     text NOT NULL,
                          program_name   varchar(255) NOT NULL,
                          description    text,
                          duration_weeks integer NOT NULL,
                          is_active      boolean DEFAULT false NOT NULL,
                          created_at     timestamp NOT NULL,
                          updated_at     timestamp,

                          CONSTRAINT programs_partners_fk
                              FOREIGN KEY (partner_id)
                                  REFERENCES partners(partner_id)
                                  ON DELETE CASCADE
                                  ON UPDATE CASCADE
);
