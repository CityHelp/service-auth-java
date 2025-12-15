-- V3__create_email_verification_codes_table.sql
-- CityHelp Auth Service - Email Verification Codes Table
-- Created: 2025-12-02

-- Create email_verification_codes table
CREATE TABLE email_verification_codes (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    code VARCHAR(6) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_used BOOLEAN NOT NULL DEFAULT FALSE,
    attempts INT NOT NULL DEFAULT 0,

    -- Foreign key constraint
    CONSTRAINT fk_verification_code_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

-- Create indexes for better performance
CREATE INDEX idx_verification_codes_user_id ON email_verification_codes(user_id);
CREATE INDEX idx_verification_codes_code ON email_verification_codes(code);
CREATE INDEX idx_verification_codes_expires_at ON email_verification_codes(expires_at);
CREATE INDEX idx_verification_codes_is_used ON email_verification_codes(is_used) WHERE is_used = FALSE;

-- Add comments for documentation
COMMENT ON TABLE email_verification_codes IS 'Email verification codes for LOCAL users (6-digit, 15-minute expiration)';
COMMENT ON COLUMN email_verification_codes.code IS '6-digit verification code sent by email';
COMMENT ON COLUMN email_verification_codes.is_used IS 'Single-use flag - marked true after successful verification';
COMMENT ON COLUMN email_verification_codes.attempts IS 'Failed verification attempts counter (max 3)';
COMMENT ON COLUMN email_verification_codes.expires_at IS 'Code expiration timestamp (15 minutes from creation)';
