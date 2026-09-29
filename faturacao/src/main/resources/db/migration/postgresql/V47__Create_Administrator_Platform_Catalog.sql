-- Catálogo persistente partilhado pelas aplicações Kubata.
CREATE TABLE IF NOT EXISTS adm_plataforma_item (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    active BOOLEAN DEFAULT TRUE NOT NULL,
    tipo VARCHAR(40) NOT NULL,
    codigo VARCHAR(120) NOT NULL,
    nome VARCHAR(255) NOT NULL,
    estado VARCHAR(40) NOT NULL DEFAULT 'ACTIVO',
    descricao VARCHAR(1000),
    config_json TEXT,
    schedule_seconds INTEGER,
    next_run_at TIMESTAMP,
    last_run_at TIMESTAMP,
    attempts INTEGER DEFAULT 0,
    last_message VARCHAR(2000),
    owner_username VARCHAR(120),
    resource_path VARCHAR(1000),
    CONSTRAINT uk_adm_plataforma_tipo_codigo UNIQUE (tipo, codigo)
);
CREATE INDEX IF NOT EXISTS idx_adm_plataforma_tipo ON adm_plataforma_item(tipo);
CREATE INDEX IF NOT EXISTS idx_adm_plataforma_estado ON adm_plataforma_item(estado);
CREATE INDEX IF NOT EXISTS idx_adm_plataforma_next_run ON adm_plataforma_item(next_run_at);
