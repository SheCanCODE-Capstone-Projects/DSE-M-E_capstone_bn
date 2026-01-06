CREATE TABLE surveys (
                         survey_id      uuid PRIMARY KEY,
                         partner_id     text NOT NULL,
                         cohort_id      uuid,
                         survey_type    varchar(20) NOT NULL,
                         title          varchar(255) NOT NULL,
                         description    text,
                         status         varchar(20) NOT NULL DEFAULT 'DRAFT',
                         created_by    uuid NOT NULL,
                         created_at    timestamp NOT NULL,
                         updated_at    timestamp,

                         CONSTRAINT check_survey_type
                             CHECK (survey_type IN ('BASELINE', 'MIDLINE', 'ENDLINE', 'TRACER')),

                         CONSTRAINT check_survey_status
                             CHECK (status IN ('DRAFT', 'PUBLISHED', 'CLOSED')),

                         CONSTRAINT surveys_partner_fk
                             FOREIGN KEY (partner_id)
                                 REFERENCES partners(partner_id)
                                 ON DELETE CASCADE
                                 ON UPDATE CASCADE,

                         CONSTRAINT surveys_cohort_fk
                             FOREIGN KEY (cohort_id)
                                 REFERENCES cohorts(cohort_id)
                                 ON DELETE SET NULL
                                 ON UPDATE CASCADE,

                         CONSTRAINT surveys_created_by_fk
                             FOREIGN KEY (created_by)
                                 REFERENCES users(user_id)
                                 ON DELETE RESTRICT
                                 ON UPDATE CASCADE
);


CREATE TABLE survey_questions (
                                  question_id    uuid PRIMARY KEY,
                                  survey_id      uuid NOT NULL,
                                  question_text  text NOT NULL,
                                  question_type  varchar(20) NOT NULL,
                                  is_required    boolean NOT NULL DEFAULT false,
                                  sequence_order integer NOT NULL,
                                  created_at     timestamp NOT NULL,

                                  CONSTRAINT check_question_type
                                      CHECK (question_type IN ('TEXT', 'NUMBER', 'SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'SCALE')),

                                  CONSTRAINT questions_surveys_fk
                                      FOREIGN KEY (survey_id)
                                          REFERENCES surveys(survey_id)
                                          ON DELETE CASCADE
                                          ON UPDATE CASCADE
);

CREATE TABLE survey_responses (
                                  response_id     uuid PRIMARY KEY,
                                  survey_id       uuid NOT NULL,
                                  participant_id  uuid NOT NULL,
                                  enrollment_id   uuid,
                                  submitted_at    timestamp NOT NULL,
                                  submitted_by    uuid,
                                  created_at      timestamp NOT NULL,

                                  CONSTRAINT unique_survey_participant
                                      UNIQUE (survey_id, participant_id),

                                  CONSTRAINT responses_surveys_fk
                                      FOREIGN KEY (survey_id)
                                          REFERENCES surveys(survey_id)
                                          ON DELETE CASCADE
                                          ON UPDATE CASCADE,

                                  CONSTRAINT responses_participants_fk
                                      FOREIGN KEY (participant_id)
                                          REFERENCES participants(participant_id)
                                          ON DELETE CASCADE
                                          ON UPDATE CASCADE,

                                  CONSTRAINT responses_enrollments_fk
                                      FOREIGN KEY (enrollment_id)
                                          REFERENCES enrollments(enrollment_id)
                                          ON DELETE SET NULL
                                          ON UPDATE CASCADE,

                                  CONSTRAINT responses_submitted_by_fk
                                      FOREIGN KEY (submitted_by)
                                          REFERENCES users(user_id)
                                          ON DELETE SET NULL
                                          ON UPDATE CASCADE
);


CREATE TABLE survey_answers (
                                answer_id     uuid PRIMARY KEY,
                                response_id   uuid NOT NULL,
                                question_id   uuid NOT NULL,
                                answer_value  text,
                                created_at    timestamp NOT NULL,

                                CONSTRAINT answers_responses_fk
                                    FOREIGN KEY (response_id)
                                        REFERENCES survey_responses(response_id)
                                        ON DELETE CASCADE
                                        ON UPDATE CASCADE,

                                CONSTRAINT answers_questions_fk
                                    FOREIGN KEY (question_id)
                                        REFERENCES survey_questions(question_id)
                                        ON DELETE CASCADE
                                        ON UPDATE CASCADE
);
