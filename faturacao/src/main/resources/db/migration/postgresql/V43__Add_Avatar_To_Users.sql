-- V43: Add avatar to users (PostgreSQL)
ALTER TABLE users ADD COLUMN avatar BYTEA;