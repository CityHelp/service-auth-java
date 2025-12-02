-- V1__create_users_table.sql
-- CityHelp Auth Service - Users Table
-- Created: 2025-12-02

-- Create enum types
CREATE TYPE oauth_provider AS ENUM ('LOCAL', 'GOOGLE');
CREATE TYPE user_status AS ENUM ('PENDING_VERIFICATION', 'ACTIVE', 'DELETED', 'SUSPENDED');
CREATE TYPE user_role AS ENUM ('USER', 'ADMIN');

-- Create users table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255),  -- NULL for OAuth2 users
    oauth_provider oauth_provider NOT NULL DEFAULT 'LOCAL',
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    status user_status NOT NULL DEFAULT 'PENDING_VERIFICATION',
    role user_role NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    last_login_at TIMESTAMP
);

-- Create indexes for better performance
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_uuid ON users(uuid);
CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_users_oauth_provider ON users(oauth_provider);
CREATE INDEX idx_users_created_at ON users(created_at DESC);

-- Add comments for documentation
COMMENT ON TABLE users IS 'CityHelp users table - supports both LOCAL and OAuth2 authentication';
COMMENT ON COLUMN users.uuid IS 'Public UUID identifier for external references';
COMMENT ON COLUMN users.password IS 'BCrypt hashed password - NULL for OAuth2 users';
COMMENT ON COLUMN users.oauth_provider IS 'Authentication provider: LOCAL (email/password) or GOOGLE (OAuth2)';
COMMENT ON COLUMN users.is_verified IS 'Email verification status - always true for OAuth2 users';
COMMENT ON COLUMN users.status IS 'Account status: PENDING_VERIFICATION → ACTIVE → DELETED/SUSPENDED';