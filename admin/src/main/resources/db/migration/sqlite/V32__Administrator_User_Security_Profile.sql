-- V32: perfil de segurança individual por utilizador.
-- A política funciona como restrição adicional ao RBAC existente.

CREATE TABLE IF NOT EXISTS user_security_profiles (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    user_id INTEGER NOT NULL UNIQUE,
    login_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    require_mfa BOOLEAN NOT NULL DEFAULT FALSE,
    allow_recovery_code BOOLEAN NOT NULL DEFAULT TRUE,
    max_login_attempts INTEGER NOT NULL DEFAULT 5,
    lockout_minutes INTEGER NOT NULL DEFAULT 15,
    session_timeout_minutes INTEGER NOT NULL DEFAULT 480,
    max_concurrent_sessions INTEGER NOT NULL DEFAULT 3,
    password_min_length INTEGER NOT NULL DEFAULT 8,
    password_require_upper BOOLEAN NOT NULL DEFAULT TRUE,
    password_require_lower BOOLEAN NOT NULL DEFAULT TRUE,
    password_require_digit BOOLEAN NOT NULL DEFAULT TRUE,
    password_require_symbol BOOLEAN NOT NULL DEFAULT FALSE,
    password_expiry_days INTEGER NOT NULL DEFAULT 90,
    login_start TIME,
    login_end TIME,
    financial_operation_limit DECIMAL(19,2),
    financial_daily_limit DECIMAL(19,2),
    financial_currency VARCHAR(3) NOT NULL DEFAULT 'AOA',
    CONSTRAINT fk_user_security_profile_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_user_security_profile_user
    ON user_security_profiles(user_id);

CREATE TABLE IF NOT EXISTS user_security_profile_companies (
    profile_id INTEGER NOT NULL,
    empresa_id INTEGER NOT NULL,
    CONSTRAINT uk_user_security_profile_company
        UNIQUE (profile_id, empresa_id),
    CONSTRAINT fk_user_security_profile_company_profile
        FOREIGN KEY (profile_id) REFERENCES user_security_profiles(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_security_profile_company_empresa
        FOREIGN KEY (empresa_id) REFERENCES empresas(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS user_security_profile_modules (
    profile_id INTEGER NOT NULL,
    modulo VARCHAR(80) NOT NULL,
    CONSTRAINT uk_user_security_profile_module
        UNIQUE (profile_id, modulo),
    CONSTRAINT fk_user_security_profile_module_profile
        FOREIGN KEY (profile_id) REFERENCES user_security_profiles(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS user_security_profile_ips (
    profile_id INTEGER NOT NULL,
    ip_range VARCHAR(64) NOT NULL,
    CONSTRAINT uk_user_security_profile_ip
        UNIQUE (profile_id, ip_range),
    CONSTRAINT fk_user_security_profile_ip_profile
        FOREIGN KEY (profile_id) REFERENCES user_security_profiles(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS user_security_profile_weekdays (
    profile_id INTEGER NOT NULL,
    weekday VARCHAR(20) NOT NULL,
    CONSTRAINT uk_user_security_profile_weekday
        UNIQUE (profile_id, weekday),
    CONSTRAINT fk_user_security_profile_weekday_profile
        FOREIGN KEY (profile_id) REFERENCES user_security_profiles(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS user_security_profile_critical_ops (
    profile_id INTEGER NOT NULL,
    operation_code VARCHAR(100) NOT NULL,
    CONSTRAINT uk_user_security_profile_critical_op
        UNIQUE (profile_id, operation_code),
    CONSTRAINT fk_user_security_profile_critical_op_profile
        FOREIGN KEY (profile_id) REFERENCES user_security_profiles(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS user_security_financial_usage (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    user_id INTEGER NOT NULL,
    usage_date DATE NOT NULL,
    currency VARCHAR(3) NOT NULL,
    amount DECIMAL(19,2) NOT NULL DEFAULT 0,
    CONSTRAINT uk_user_security_financial_usage_day
        UNIQUE (user_id, usage_date, currency),
    CONSTRAINT fk_user_security_financial_usage_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_user_security_financial_usage_user_date
    ON user_security_financial_usage(user_id, usage_date);
