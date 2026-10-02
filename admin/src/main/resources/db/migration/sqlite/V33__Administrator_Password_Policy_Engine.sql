-- V33: motor central de políticas de palavra-passe e histórico de credenciais.
-- A resolução server-side segue: UTILIZADOR -> PERFIL -> EMPRESA -> GLOBAL.
-- scope_key evita múltiplas políticas activas no mesmo âmbito, incluindo GLOBAL.
-- O timestamp de reset permite expiração exacta de credenciais temporárias.

ALTER TABLE users ADD COLUMN password_reset_expires_at TIMESTAMP;

CREATE TABLE IF NOT EXISTS password_policies (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    scope_type VARCHAR(20) NOT NULL,
    scope_id INTEGER,
    scope_key VARCHAR(80) NOT NULL UNIQUE,
    nome VARCHAR(120) NOT NULL DEFAULT 'Política de palavra-passe',
    min_length INTEGER NOT NULL DEFAULT 8,
    max_length INTEGER NOT NULL DEFAULT 128,
    require_upper BOOLEAN NOT NULL DEFAULT TRUE,
    require_lower BOOLEAN NOT NULL DEFAULT TRUE,
    require_digit BOOLEAN NOT NULL DEFAULT TRUE,
    require_symbol BOOLEAN NOT NULL DEFAULT FALSE,
    history_count INTEGER NOT NULL DEFAULT 5,
    expiry_days INTEGER NOT NULL DEFAULT 90,
    minimum_age_hours INTEGER NOT NULL DEFAULT 0,
    reset_validity_hours INTEGER NOT NULL DEFAULT 24,
    force_change_on_reset BOOLEAN NOT NULL DEFAULT TRUE,
    prohibit_identity_fragments BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_password_policy_scope
    ON password_policies(scope_type, scope_id);

CREATE TABLE IF NOT EXISTS password_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    user_id INTEGER NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    changed_at TIMESTAMP NOT NULL,
    changed_by VARCHAR(120),
    reason VARCHAR(60),
    CONSTRAINT fk_password_history_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_password_history_user_changed
    ON password_history(user_id, changed_at DESC);
