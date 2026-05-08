-- Alterar a tabela backup_record para permitir filename nulo temporariamente (SQLite não suporta ALTER COLUMN para NULL)
-- A abordagem padrão no SQLite é criar uma nova tabela, copiar os dados e renomear.

CREATE TABLE backup_record_new (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    filename VARCHAR(200), -- Removido NOT NULL
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

INSERT INTO backup_record_new (id, active, created_at, updated_at, filename, type, status, triggered_by, start_time, end_time, file_size, checksum, db_url, compressed, encrypted, error_message, restore_count, restored_at, restored_by, notes)
SELECT id, active, created_at, updated_at, filename, type, status, triggered_by, start_time, end_time, file_size, checksum, db_url, compressed, encrypted, error_message, restore_count, restored_at, restored_by, notes FROM backup_record;

DROP TABLE backup_record;
ALTER TABLE backup_record_new RENAME TO backup_record;

CREATE INDEX IF NOT EXISTS idx_backup_status ON backup_record(status);
CREATE INDEX IF NOT EXISTS idx_backup_time ON backup_record(start_time);
