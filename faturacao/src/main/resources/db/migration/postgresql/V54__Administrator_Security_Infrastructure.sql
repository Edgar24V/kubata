-- V54: infraestrutura de autenticação/segurança do core usada pelo Faturação.
CREATE TABLE IF NOT EXISTS user_devices (
    id BIGSERIAL PRIMARY KEY,
    created_at BIGINT NOT NULL,
    updated_at BIGINT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    user_id BIGINT NOT NULL,
    device_key VARCHAR(128) NOT NULL,
    device_name VARCHAR(120) NOT NULL,
    device_type VARCHAR(40),
    platform VARCHAR(120),
    first_seen BIGINT NOT NULL,
    last_seen BIGINT NOT NULL,
    last_ip VARCHAR(45),
    trusted BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at BIGINT,
    notes VARCHAR(500),
    CONSTRAINT uk_user_device_key UNIQUE (user_id, device_key),
    CONSTRAINT fk_user_device_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_user_device_user ON user_devices(user_id);
CREATE INDEX IF NOT EXISTS idx_user_device_last_seen ON user_devices(last_seen);
CREATE INDEX IF NOT EXISTS idx_user_device_active ON user_devices(active);

CREATE TABLE IF NOT EXISTS user_sessions (
    id BIGSERIAL PRIMARY KEY,
    created_at BIGINT NOT NULL,
    updated_at BIGINT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    username VARCHAR(255),
    workstation VARCHAR(255),
    ip_address VARCHAR(45),
    context VARCHAR(255),
    login_time BIGINT,
    memory_usage VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_user_sessions_username ON user_sessions(username);
CREATE INDEX IF NOT EXISTS idx_user_sessions_active ON user_sessions(active);

CREATE TABLE IF NOT EXISTS user_security_profiles (
    id BIGSERIAL PRIMARY KEY,
    created_at BIGINT NOT NULL,
    updated_at BIGINT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    user_id BIGINT NOT NULL UNIQUE,
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
    CONSTRAINT uk_user_security_profile_user UNIQUE (user_id),
    CONSTRAINT fk_user_security_profile_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS user_security_profile_companies (
    profile_id BIGINT NOT NULL,
    empresa_id BIGINT NOT NULL,
    CONSTRAINT uk_user_security_profile_company UNIQUE (profile_id, empresa_id),
    CONSTRAINT fk_user_security_profile_company_profile FOREIGN KEY (profile_id)
        REFERENCES user_security_profiles(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS user_security_profile_modules (
    profile_id BIGINT NOT NULL,
    modulo VARCHAR(80) NOT NULL,
    CONSTRAINT uk_user_security_profile_module UNIQUE (profile_id, modulo),
    CONSTRAINT fk_user_security_profile_module_profile FOREIGN KEY (profile_id)
        REFERENCES user_security_profiles(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS user_security_profile_ips (
    profile_id BIGINT NOT NULL,
    ip_range VARCHAR(64) NOT NULL,
    CONSTRAINT uk_user_security_profile_ip UNIQUE (profile_id, ip_range),
    CONSTRAINT fk_user_security_profile_ip_profile FOREIGN KEY (profile_id)
        REFERENCES user_security_profiles(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS user_security_profile_weekdays (
    profile_id BIGINT NOT NULL,
    weekday VARCHAR(20) NOT NULL,
    CONSTRAINT uk_user_security_profile_weekday UNIQUE (profile_id, weekday),
    CONSTRAINT fk_user_security_profile_weekday_profile FOREIGN KEY (profile_id)
        REFERENCES user_security_profiles(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS user_security_profile_critical_ops (
    profile_id BIGINT NOT NULL,
    operation_code VARCHAR(100) NOT NULL,
    CONSTRAINT uk_user_security_profile_critical_op UNIQUE (profile_id, operation_code),
    CONSTRAINT fk_user_security_profile_critical_op_profile FOREIGN KEY (profile_id)
        REFERENCES user_security_profiles(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS user_security_financial_usage (
    id BIGSERIAL PRIMARY KEY,
    created_at BIGINT NOT NULL,
    updated_at BIGINT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    user_id BIGINT NOT NULL,
    usage_date BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    amount DECIMAL(19,2) NOT NULL DEFAULT 0,
    CONSTRAINT uk_user_security_financial_usage_day UNIQUE (user_id, usage_date, currency),
    CONSTRAINT fk_user_security_financial_usage_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_user_security_financial_usage_user_date
    ON user_security_financial_usage(user_id, usage_date);
