-- Criação das tabelas de Auditoria, Logs e Backup para normas AGT/SAFT-AO
-- V28__Create_Audit_Log_Backup_Tables.sql

-- Tabela de Auditoria (rastreamento de operações)
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

-- Índices para audit_log
CREATE INDEX IF NOT EXISTS idx_audit_user ON audit_log(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_action ON audit_log(action_type);
CREATE INDEX IF NOT EXISTS idx_audit_entity ON audit_log(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_timestamp ON audit_log(timestamp);
CREATE INDEX IF NOT EXISTS idx_audit_saft ON audit_log(saft_relevant);

-- Tabela de Logs do Sistema
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

-- Índices para system_log
CREATE INDEX IF NOT EXISTS idx_syslog_level ON system_log(log_level);
CREATE INDEX IF NOT EXISTS idx_syslog_category ON system_log(category);
CREATE INDEX IF NOT EXISTS idx_syslog_timestamp ON system_log(timestamp);
CREATE INDEX IF NOT EXISTS idx_syslog_source ON system_log(source);

-- Tabela de Registros de Backup
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
    restored_at TIMESTAMP,
    restored_by VARCHAR(100),
    notes VARCHAR(500)
);

-- Índices para backup_record
CREATE INDEX IF NOT EXISTS idx_backup_status ON backup_record(status);
CREATE INDEX IF NOT EXISTS idx_backup_time ON backup_record(start_time);
