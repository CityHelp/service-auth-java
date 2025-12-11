-- Add account lockout fields to users table
ALTER TABLE users
ADD COLUMN failed_login_attempts INTEGER DEFAULT 0 NOT NULL,
ADD COLUMN locked_until TIMESTAMP,
ADD COLUMN last_failed_login_attempt TIMESTAMP;

-- Create index for faster lockout queries
CREATE INDEX IF NOT EXISTS idx_users_locked_until ON users(locked_until);
