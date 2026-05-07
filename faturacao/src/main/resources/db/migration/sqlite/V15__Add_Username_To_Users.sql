ALTER TABLE users ADD COLUMN username VARCHAR(255);

UPDATE users
SET username = email
WHERE username IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_users_username ON users(username);

