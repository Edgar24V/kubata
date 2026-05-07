CREATE TABLE user_access_permission_new (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    active INTEGER NOT NULL DEFAULT 1,
    user_id INTEGER NOT NULL,
    modulo VARCHAR(64) NOT NULL,
    opcao  VARCHAR(64) NOT NULL,
    CONSTRAINT uk_user_mod_op UNIQUE (user_id, modulo, opcao),
    CONSTRAINT fk_uap_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

INSERT INTO user_access_permission_new (id, created_at, updated_at, active, user_id, modulo, opcao)
SELECT id, created_at, updated_at, active, user_id, modulo, opcao FROM user_access_permission;

DROP TABLE user_access_permission;
ALTER TABLE user_access_permission_new RENAME TO user_access_permission;
CREATE INDEX IF NOT EXISTS idx_uap_user ON user_access_permission(user_id);
