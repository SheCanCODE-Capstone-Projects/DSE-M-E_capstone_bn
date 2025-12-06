CREATE TABLE role_requests
(
    id UUID PRIMARY KEY,
    requester_id   UUID NOT NULL,
    partner_id     VARCHAR(50) NOT NULL,
    center_id      UUID NOT NULL,
    requested_role VARCHAR(20) NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    requested_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    approved_by    UUID,
    approved_at    TIMESTAMP,
    admin_comment  TEXT,


    CONSTRAINT check_requested_role
        CHECK (requested_role IN ('PARTNER', 'ME_OFFICER', 'FACILITATOR')),

    CONSTRAINT check_request_status
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),

    CONSTRAINT fk_role_request_requester
        FOREIGN KEY (requester_id)
            REFERENCES users(user_id)
            ON DELETE CASCADE
            ON UPDATE CASCADE,

    CONSTRAINT fk_role_request_approved_by
        FOREIGN KEY (approved_by)
            REFERENCES users(user_id)
            ON DELETE SET NULL
            ON UPDATE CASCADE,

    CONSTRAINT fk_role_request_partner
        FOREIGN KEY (partner_id)
            REFERENCES partners(partner_id)
            ON DELETE RESTRICT
            ON UPDATE CASCADE,

    CONSTRAINT fk_role_request_center
        FOREIGN KEY (center_id)
            REFERENCES centers(center_id)
            ON DELETE RESTRICT
            ON UPDATE CASCADE
);

ALTER TABLE role_requests
ADD CONSTRAINT unique_role_request
    UNIQUE (requester_id, requested_role, partner_id, center_id);


