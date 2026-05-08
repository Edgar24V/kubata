CREATE TABLE IF NOT EXISTS user_sessions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    username VARCHAR(100) NOT NULL,
    workstation VARCHAR(100),
    ip_address VARCHAR(50),
    context VARCHAR(200),
    login_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    memory_usage VARCHAR(50)
);

CREATE INDEX IF NOT EXISTS idx_user_sessions_username ON user_sessions(username);
