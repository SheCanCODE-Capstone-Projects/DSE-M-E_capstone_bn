CREATE TABLE audit_logs (
    audit_log_id  uuid PRIMARY KEY,
    actor_id      uuid NOT NULL,
    actor_role    varchar(30) NOT NULL,
    action        varchar(100) NOT NULL,
    entity_type   varchar(50) NOT NULL,
    entity_id     uuid,
    description   text,
    created_at    timestamp NOT NULL DEFAULT now(),

    CONSTRAINT audit_logs_users_fk
        FOREIGN KEY (actor_id)
            REFERENCES users(user_id)
            ON DELETE RESTRICT
            ON UPDATE CASCADE
);

-- Add index for common queries
CREATE INDEX idx_audit_logs_actor_id ON audit_logs(actor_id);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);

