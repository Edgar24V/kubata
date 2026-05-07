-- V41: Add failed_attempts and lockout_end to users (PostgreSQL)
ALTER TABLE users ADD COLUMN failed_attempts INTEGER DEFAULT 0;
ALTER TABLE users ADD COLUMN lockout_end TIMESTAMP;
