-- Create alerts table for system alerts and notifications
-- Used for reactive flagging of inconsistencies across the platform

CREATE TABLE IF NOT EXISTS alerts (
    alert_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    partner_id TEXT NOT NULL,
    recipient_id UUID,
    severity VARCHAR(20) NOT NULL,
    alert_type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    issue_count INTEGER NOT NULL DEFAULT 0,
    call_to_action VARCHAR(100),
    related_entity_type VARCHAR(50),
    related_entity_id UUID,
    is_resolved BOOLEAN NOT NULL DEFAULT FALSE,
    resolved_at TIMESTAMP,
    resolved_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT alerts_partner_fk
        FOREIGN KEY (partner_id) REFERENCES partners(partner_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT alerts_recipient_fk
        FOREIGN KEY (recipient_id) REFERENCES users(user_id)
        ON DELETE SET NULL
        ON UPDATE CASCADE,
    CONSTRAINT alerts_resolved_by_fk
        FOREIGN KEY (resolved_by) REFERENCES users(user_id)
        ON DELETE SET NULL
        ON UPDATE CASCADE,
    CONSTRAINT check_alert_severity
        CHECK (severity IN ('CRITICAL', 'WARNING', 'INFO'))
);

-- Create indexes for better query performance
CREATE INDEX idx_alerts_partner ON alerts(partner_id);
CREATE INDEX idx_alerts_recipient ON alerts(recipient_id);
CREATE INDEX idx_alerts_severity ON alerts(severity);
CREATE INDEX idx_alerts_resolved ON alerts(is_resolved);
CREATE INDEX idx_alerts_created_at ON alerts(created_at DESC);
