-- Schema mínimo para Kubata Administrator (SQLite)

CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    nif VARCHAR(20),
    telefone VARCHAR(20),
    mfa_secret VARCHAR(255),
    mfa_enabled BOOLEAN DEFAULT FALSE,
    ultimo_acesso TIMESTAMP,
    failed_attempts INTEGER DEFAULT 0,
    lockout_end TIMESTAMP,
    password_changed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    active BOOLEAN DEFAULT TRUE NOT NULL
);

CREATE TABLE IF NOT EXISTS user_access_permission (
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

CREATE INDEX IF NOT EXISTS idx_uap_user ON user_access_permission(user_id);

CREATE TABLE IF NOT EXISTS audit_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    user_id INTEGER,
    username VARCHAR(100) NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    entity_type VARCHAR(100),
    entity_id VARCHAR(100),
    entity_description VARCHAR(500),
    old_values TEXT,
    new_values TEXT,
    ip_address VARCHAR(50),
    user_agent VARCHAR(500),
    session_id VARCHAR(100),
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    success BOOLEAN DEFAULT TRUE,
    error_message VARCHAR(1000),
    module VARCHAR(50),
    saft_relevant BOOLEAN DEFAULT FALSE,
    agt_compliance_level VARCHAR(20) DEFAULT 'NORMAL',
    hash_integrity VARCHAR(64),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_audit_user ON audit_log(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_action ON audit_log(action_type);
CREATE INDEX IF NOT EXISTS idx_audit_entity ON audit_log(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_timestamp ON audit_log(timestamp);
CREATE INDEX IF NOT EXISTS idx_audit_saft ON audit_log(saft_relevant);

CREATE TABLE IF NOT EXISTS system_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    log_level VARCHAR(20) NOT NULL,
    category VARCHAR(50),
    source VARCHAR(100),
    message VARCHAR(2000) NOT NULL,
    exception_class VARCHAR(200),
    stack_trace TEXT,
    thread_name VARCHAR(100),
    class_name VARCHAR(200),
    method_name VARCHAR(100),
    line_number INTEGER,
    memory_used_mb INTEGER,
    cpu_usage_percent REAL,
    correlation_id VARCHAR(50)
);

CREATE INDEX IF NOT EXISTS idx_syslog_level ON system_log(log_level);
CREATE INDEX IF NOT EXISTS idx_syslog_category ON system_log(category);
CREATE INDEX IF NOT EXISTS idx_syslog_timestamp ON system_log(timestamp);
CREATE INDEX IF NOT EXISTS idx_syslog_source ON system_log(source);

CREATE TABLE IF NOT EXISTS backup_record (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    filename VARCHAR(200) NOT NULL,
    type VARCHAR(50),
    status VARCHAR(20) NOT NULL,
    triggered_by VARCHAR(100),
    start_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    end_time TIMESTAMP,
    file_size INTEGER,
    checksum VARCHAR(64),
    db_url VARCHAR(500),
    compressed BOOLEAN DEFAULT TRUE,
    encrypted BOOLEAN DEFAULT FALSE,
    error_message VARCHAR(1000),
    restore_count INTEGER DEFAULT 0,
    restored_at TIMESTAMP,
    restored_by VARCHAR(100),
    notes VARCHAR(500)
);

CREATE INDEX IF NOT EXISTS idx_backup_status ON backup_record(status);
CREATE INDEX IF NOT EXISTS idx_backup_time ON backup_record(start_time);

CREATE TABLE IF NOT EXISTS backup_config (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    enabled BOOLEAN DEFAULT TRUE,
    frequency VARCHAR(20) DEFAULT 'DAILY',
    schedule_time VARCHAR(8) DEFAULT '02:00:00',
    retention_days INTEGER DEFAULT 30,
    backup_location VARCHAR(500) DEFAULT 'backups',
    compress_backup BOOLEAN DEFAULT TRUE,
    include_attachments BOOLEAN DEFAULT TRUE,
    notify_on_success BOOLEAN DEFAULT FALSE,
    notify_on_failure BOOLEAN DEFAULT TRUE,
    email_notifications VARCHAR(500),
    last_execution TIMESTAMP,
    next_execution TIMESTAMP,
    last_status VARCHAR(20),
    last_error_message VARCHAR(1000)
);

INSERT OR IGNORE INTO backup_config (id, enabled, frequency, schedule_time, retention_days)
VALUES (1, 1, 'DAILY', '02:00:00', 30);

CREATE TABLE IF NOT EXISTS empresas (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    nome        TEXT NOT NULL,
    nif         TEXT,
    nif_fiscal  TEXT,
    nif_seguranca_social TEXT,
    email       TEXT,
    telefone    TEXT,
    fax         TEXT,
    website     TEXT,
    morada      TEXT,
    codigo_postal TEXT,
    locality    TEXT,
    cae         TEXT,
    capital_social REAL,
    regime_fiscal TEXT,
    modulos     TEXT,
    ativa       INTEGER DEFAULT 1,
    active      INTEGER DEFAULT 1,
    created_at  TEXT,
    updated_at  TEXT
);
