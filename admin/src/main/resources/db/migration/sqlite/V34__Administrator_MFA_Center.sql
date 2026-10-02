-- V34: política centralizada de MFA.
-- A resolução server-side usa UTILIZADOR -> PERFIL -> EMPRESA -> GLOBAL,
-- com fallback para UserSecurityProfile.require_mfa.

CREATE TABLE IF NOT EXISTS mfa_policies (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    scope_type VARCHAR(20) NOT NULL,
    scope_id INTEGER,
    scope_key VARCHAR(80) NOT NULL UNIQUE,
    nome VARCHAR(120) NOT NULL DEFAULT 'Política MFA',
    required BOOLEAN NOT NULL DEFAULT FALSE,
    allow_user_disable BOOLEAN NOT NULL DEFAULT TRUE,
    allow_recovery_codes BOOLEAN NOT NULL DEFAULT TRUE,
    recovery_code_count INTEGER NOT NULL DEFAULT 10,
    issuer VARCHAR(80) NOT NULL DEFAULT 'Kubata',
    grace_period_days INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_mfa_policy_scope
    ON mfa_policies(scope_type, scope_id);
