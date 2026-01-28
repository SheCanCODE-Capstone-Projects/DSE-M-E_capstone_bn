-- Create report snapshots table for storing monthly partner reports
CREATE TABLE report_snapshots (
    snapshot_id          uuid PRIMARY KEY,
    partner_id           text NOT NULL,
    report_type          varchar(50) NOT NULL,
    report_period_start  date NOT NULL,
    report_period_end    date NOT NULL,
    report_data          text NOT NULL, -- JSON or CSV data stored as text
    file_format          varchar(10) NOT NULL, -- CSV, PDF, JSON
    file_size_bytes      bigint,
    generated_by         uuid NOT NULL,
    generated_at         timestamp NOT NULL DEFAULT now(),
    created_at           timestamp NOT NULL DEFAULT now(),

    CONSTRAINT check_report_type
        CHECK (report_type IN ('MONTHLY_PARTNER_REPORT', 'QUARTERLY_PARTNER_REPORT', 'ANNUAL_PARTNER_REPORT', 'CUSTOM')),

    CONSTRAINT check_file_format
        CHECK (file_format IN ('CSV', 'PDF', 'JSON')),

    CONSTRAINT report_snapshots_partners_fk
        FOREIGN KEY (partner_id)
            REFERENCES partners(partner_id)
            ON DELETE CASCADE
            ON UPDATE CASCADE,

    CONSTRAINT report_snapshots_users_fk
        FOREIGN KEY (generated_by)
            REFERENCES users(user_id)
            ON DELETE RESTRICT
            ON UPDATE CASCADE
);

-- Add indexes
CREATE INDEX idx_report_snapshots_partner_id ON report_snapshots(partner_id);
CREATE INDEX idx_report_snapshots_report_type ON report_snapshots(report_type);
CREATE INDEX idx_report_snapshots_period ON report_snapshots(report_period_start, report_period_end);
CREATE INDEX idx_report_snapshots_generated_at ON report_snapshots(generated_at);
