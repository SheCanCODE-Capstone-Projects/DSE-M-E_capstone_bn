-- Add organization (partner) and location (center) fields to access_requests table
-- This allows facilitators to specify which organization and branch they work for
-- so ME_OFFICERs can filter requests by their organization and location

ALTER TABLE access_requests
ADD COLUMN IF NOT EXISTS organization_partner_id TEXT,
ADD COLUMN IF NOT EXISTS location_center_id UUID;

-- Add foreign key constraints
ALTER TABLE access_requests
ADD CONSTRAINT fk_access_request_partner
    FOREIGN KEY (organization_partner_id)
    REFERENCES partners(partner_id)
    ON DELETE SET NULL
    ON UPDATE CASCADE;

ALTER TABLE access_requests
ADD CONSTRAINT fk_access_request_center
    FOREIGN KEY (location_center_id)
    REFERENCES centers(center_id)
    ON DELETE SET NULL
    ON UPDATE CASCADE;

-- Add indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_access_requests_organization ON access_requests(organization_partner_id);
CREATE INDEX IF NOT EXISTS idx_access_requests_location ON access_requests(location_center_id);
