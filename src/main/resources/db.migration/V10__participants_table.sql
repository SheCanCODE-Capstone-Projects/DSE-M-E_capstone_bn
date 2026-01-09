CREATE TABLE participants (
                              participant_id             uuid PRIMARY KEY,
                              partner_id                 text NOT NULL,
                              first_name                 varchar(100) NOT NULL,
                              last_name                  varchar(100) NOT NULL,
                              email                      varchar(255) NOT NULL,
                              phone                      varchar(255) NOT NULL,
                              date_of_birth              date,
                              gender                     varchar(20) NOT NULL,
                              disability_status          varchar(20) NOT NULL,
                              education_level            varchar(100) NOT NULL,
                              employment_status_baseline varchar(100) NOT NULL,
                              created_at                 timestamp NOT NULL,
                              updated_at                 timestamp,

                              CONSTRAINT check_gender
                                  CHECK (gender IN ('MALE', 'FEMALE', 'NON_BINARY', 'PREFER_NOT_TO_SAY')),

                              CONSTRAINT check_disability_status
                                  CHECK (disability_status IN ('YES', 'NO', 'PREFER_NOT_TO_SAY')),

                              CONSTRAINT participants_partners_fk
                                  FOREIGN KEY (partner_id)
                                      REFERENCES partners(partner_id)
                                      ON DELETE CASCADE
                                      ON UPDATE CASCADE
);


ALTER TABLE participants
    ADD CONSTRAINT check_employment_status
        CHECK (employment_status_baseline IN ('EMPLOYED', 'UNEMPLOYED', 'SELF_EMPLOYED', 'FURTHER_EDUCATION'));
