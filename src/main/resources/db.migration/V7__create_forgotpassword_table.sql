-- Create forgotpassword table for password reset functionality
CREATE TABLE IF NOT EXISTS forgotpassword (
    id SERIAL PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    expiration_time TIMESTAMP NOT NULL,
    user_id UUID NOT NULL,
    CONSTRAINT fk_forgotpassword_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Create index for faster token lookups
CREATE INDEX IF NOT EXISTS idx_forgotpassword_token ON forgotpassword(token);
CREATE INDEX IF NOT EXISTS idx_forgotpassword_user ON forgotpassword(user_id);

