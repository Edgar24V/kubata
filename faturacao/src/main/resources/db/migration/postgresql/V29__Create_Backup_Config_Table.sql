-- Criação da tabela de configurações de backup automático
-- V29__Create_Backup_Config_Table.sql

CREATE TABLE IF NOT EXISTS backup_config (
    id BIGSERIAL PRIMARY KEY,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    enabled BOOLEAN DEFAULT TRUE,
    frequency VARCHAR(20) DEFAULT 'DAILY',
    schedule_time TIME DEFAULT '02:00:00',
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

-- Inserir configuração padrão sem fixar o ID: mantém a sequência BIGSERIAL coerente.
-- A sintaxe é compatível com PostgreSQL e com H2 em modo PostgreSQL para os testes.
INSERT INTO backup_config (enabled, frequency, schedule_time, retention_days)
SELECT TRUE, 'DAILY', TIME '02:00:00', 30
WHERE NOT EXISTS (SELECT 1 FROM backup_config);
