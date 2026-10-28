-- V15: username em users
-- O ALTER foi incorporado no esquema canónico de users (V1).
UPDATE users
SET username = email
WHERE username IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_users_username ON users(username);
