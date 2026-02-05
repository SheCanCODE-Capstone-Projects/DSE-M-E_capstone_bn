-- Create email_change_tokens table for tracking email change requests
CREATE TABLE IF NOT EXISTS email_change_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    new_email VARCHAR(255) NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create index on token for faster lookups
CREATE INDEX IF NOT EXISTS idx_email_change_tokens_token ON email_change_tokens(token);
CREATE INDEX IF NOT EXISTS idx_email_change_tokens_user_id ON email_change_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_email_change_tokens_expiry_date ON email_change_tokens(expiry_date);
